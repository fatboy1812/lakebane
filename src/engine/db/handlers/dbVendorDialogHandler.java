// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.gameManager.DbManager;
import engine.objects.VendorDialog;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbVendorDialogHandler extends dbHandlerBase {

	public dbVendorDialogHandler() {
		this.localClass = VendorDialog.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public VendorDialog GET_VENDORDIALOG(final int objectUUID) {

		VendorDialog vendorDialog = (VendorDialog) DbManager.getFromCache(Enum.GameObjectType.VendorDialog, objectUUID);

		if (vendorDialog != null)
			return vendorDialog;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_vendordialog` WHERE `ID`=?")) {

			preparedStatement.setInt(1, objectUUID);

			ResultSet rs = preparedStatement.executeQuery();
			vendorDialog = (VendorDialog) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return vendorDialog;
	}
}
