package app.oengus.service;

import app.oengus.entity.dto.DonationStatsDto;
import app.oengus.entity.model.Donation;
import app.oengus.entity.model.Incentive;
import app.oengus.entity.model.Marathon;
import app.oengus.exception.OengusBusinessException;
import app.oengus.helper.BeanHelper;
import app.oengus.service.repository.BidRepositoryService;
import app.oengus.service.repository.DonationRepositoryService;
import app.oengus.service.webhook.AbstractWebhookService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.*;
import javassist.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class DonationService {

	@Autowired
	private DonationRepositoryService donationRepositoryService;

	@Autowired
	private MarathonService marathonService;

	@Autowired
	private BidRepositoryService bidRepositoryService;

	@Autowired
	private PayPalHttpClient payPalHttpClient;

	@Autowired
	private AbstractWebhookService donationWebhook;

	public Page<Donation> findForMarathon(final String marathonId, final Integer page, final Integer size) {
		return this.donationRepositoryService.findByMarathon(marathonId,
				PageRequest.of(page, size, Sort.by(List.of(Sort.Order.desc("date")))));
	}

	@Transactional
	public Order initDonation(final String marathonId, final Donation donation) {
		try {
			final Marathon marathon = this.marathonService.findOne(marathonId);
			if (!marathon.isHasDonations()) {
				throw new OengusBusinessException("NO_DONATIONS");
			}
			final Order order = this.createOrder(marathon, donation.getAmount().toPlainString());
			donation.setFunctionalId(order.id());
			donation.setApproved(false);
			donation.setDate(ZonedDateTime.now());
			donation.setMarathon(marathon);
			donation.setPaymentSource("PAYPAL");
			donation.getAnswers().forEach(donationExtraData -> donationExtraData.setDonation(donation));
			donation.getDonationIncentiveLinks()
			        .forEach(donationIncentiveLink -> {
				        donationIncentiveLink.setDonation(donation);
				        if (donationIncentiveLink.getBid() != null && donationIncentiveLink.getBid().getId() == null) {
					        final Incentive incentive = new Incentive();
					        incentive.setId(donationIncentiveLink.getBid().getIncentiveId());
					        donationIncentiveLink.getBid().setIncentive(incentive);
					        donationIncentiveLink.setBid(
							        this.bidRepositoryService.save(donationIncentiveLink.getBid()));
				        }
			        });
			if (StringUtils.isEmpty(donation.getNickname())) {
				donation.setNickname("Anonymous");
			}
			this.donationRepositoryService.save(donation);
			return order;
		} catch (final NotFoundException e) {
			throw new OengusBusinessException("MARATHON_NOT_FOUND");
		}
	}

	private Order createOrder(final Marathon marathon, final String amount) {
		final OrderRequest orderRequest = new OrderRequest();
		orderRequest.checkoutPaymentIntent("CAPTURE");
		final Money money = new Money().value(amount)
		                               .currencyCode(
				                               marathon.getDonationCurrency());
		final PurchaseUnitRequest purchaseUnitRequest =
				new PurchaseUnitRequest()
						.amountWithBreakdown(new AmountWithBreakdown()
								.value(amount)
								.currencyCode(marathon.getDonationCurrency())
								.amountBreakdown(new AmountBreakdown().itemTotal(money)))
						.items(List.of(new Item().name("Donation to " + marathon.getSupportedCharity())
						                         .quantity("1")
						                         .category("DIGITAL_GOODS")
						                         .unitAmount(money)));
		final Payee payee = new Payee();
		payee.email(marathon.getPayee());
		purchaseUnitRequest.payee(payee);
		orderRequest.purchaseUnits(List.of(purchaseUnitRequest));
		final OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
		try {
			return this.payPalHttpClient.execute(request).result();
		} catch (final IOException | NullPointerException e) {
			if (e instanceof HttpException) {
				// Something went wrong server-side
				final HttpException he = (HttpException) e;
				LoggerFactory.getLogger(DonationService.class).error(he.getMessage());
				he.headers()
				  .forEach(
						  x -> LoggerFactory.getLogger(DonationService.class).error(x + " :" + he.headers().header(x)));
			}
			throw new OengusBusinessException("ERROR_DONATION_CREATION");
		}
	}

	public void approveDonation(final String marathonId, final String orderId) {
		final OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);

		try {
			final Marathon marathon = this.marathonService.findOne(marathonId);
			// Call API with your client and get a response for your call
			final HttpResponse<Order> response = this.payPalHttpClient.execute(request);

			// If call returns body in response, you can get the de-serialized version by
			// calling result() on the response
			final Order order = response.result();

			if (order.purchaseUnits().get(0).payments().captures().get(0).status().equals("COMPLETED")) {
				final Donation donation = this.donationRepositoryService.findByFunctionalId(order.id());
				donation.setApproved(true);
				this.donationRepositoryService.save(donation);

				if (StringUtils.isNotEmpty(marathon.getWebhook())) {
					this.sendDonationEvent(marathon.getWebhook(), donation);
				}
			}
		} catch (final IOException ioe) {
			if (ioe instanceof HttpException) {
				// Something went wrong server-side
				final HttpException he = (HttpException) ioe;
				LoggerFactory.getLogger(DonationService.class).error(he.getMessage());
				he.headers()
				  .forEach(
						  x -> LoggerFactory.getLogger(DonationService.class).error(x + " :" + he.headers().header(x)));
			}
			throw new OengusBusinessException("ERROR_DONATION_VALIDATION");
		} catch (final NotFoundException e) {
			throw new OengusBusinessException("MARATHON_NOT_FOUND");
		}
	}

	@Transactional
	public void deleteDonation(final String orderId) {
		this.donationRepositoryService.delete(orderId);
	}

	public DonationStatsDto getStats(final String marathonId) {
		final DonationStatsDto donationStatsDto = new DonationStatsDto();
		donationStatsDto.setAverage(this.donationRepositoryService.findAverageAmountByMarathon(marathonId));
		donationStatsDto.setMax(this.donationRepositoryService.findMaxAmountByMarathon(marathonId));
		donationStatsDto.setTotal(this.donationRepositoryService.findTotalAmountByMarathon(marathonId));
		donationStatsDto.setCount(this.donationRepositoryService.countByMarathon(marathonId));
		return donationStatsDto;
	}

	private void sendDonationEvent(final String url, final Donation donation) {
	    // copy the donation to prevent modifying a real donation
	    final Donation parsedDonation = new Donation();

        BeanHelper.copyProperties(donation, parsedDonation);

        parsedDonation.setFunctionalId(null);
        parsedDonation.setMarathon(null);
        parsedDonation.setPaymentSource(null);
        parsedDonation.setApproved(null);
        parsedDonation.setAnswers(null);
        parsedDonation.setDonationIncentiveLinks(null);
        parsedDonation.setTest(false);
		try {
			this.donationWebhook.sendDonationEvent(url, parsedDonation);
		} catch (final IOException e) {
			LoggerFactory.getLogger(DonationService.class).error(e.getLocalizedMessage());
		}
	}

}
