// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.PromotionClass;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbPromotionClassHandler extends dbHandlerBase {

    public dbPromotionClassHandler() {
        this.localClass = PromotionClass.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public ArrayList<Integer> GET_ALLOWED_RUNES(final PromotionClass pc) {

        ArrayList<Integer> runeList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_promotionrunereq` WHERE `promoID`=?")) {

            preparedStatement.setInt(1, pc.getObjectUUID());

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                runeList.add(rs.getInt("runereqID"));

        } catch (SQLException e) {
            Logger.error(e);

        }
        return runeList;
    }

    public PromotionClass GET_PROMOTION_CLASS(final int objectUUID) {

        PromotionClass promotionClass = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_promotion` WHERE `ID` = ?")) {

            preparedStatement.setInt(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();
            promotionClass = (PromotionClass) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return promotionClass;
    }

    public ArrayList<PromotionClass> GET_ALL_PROMOTIONS() {

        ArrayList<PromotionClass> promotionList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_promotion`")) {

            ResultSet rs = preparedStatement.executeQuery();
            promotionList = getObjectsFromRs(rs, 30);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return promotionList;
    }
}
