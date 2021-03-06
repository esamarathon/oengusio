package app.oengus.dao;

import app.oengus.entity.model.DonationExtraData;
import app.oengus.entity.model.Marathon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationExtraDataRepository extends JpaRepository<DonationExtraData, Integer> {

    @Modifying
    @Query("DELETE FROM DonationExtraData ded WHERE ded.id IN (SELECT sded.id FROM DonationExtraData sded WHERE sded" +
        ".donation.marathon = :marathon)")
    void deleteByMarathon(@Param(value = "marathon") Marathon marathon);

}
