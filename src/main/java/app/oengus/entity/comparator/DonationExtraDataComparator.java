package app.oengus.entity.comparator;

import app.oengus.entity.model.DonationExtraData;

import java.util.Comparator;

public class DonationExtraDataComparator implements Comparator<DonationExtraData> {

	@Override
	public int compare(final DonationExtraData o1, final DonationExtraData o2) {
		return Integer.compare(o1.getQuestion().getPosition(), o2.getQuestion().getPosition());
	}
}
