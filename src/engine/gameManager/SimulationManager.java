// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.gameManager;

import engine.Enum;
import engine.Enum.GameObjectType;
import engine.objects.AbstractGameObject;
import engine.objects.City;
import engine.objects.PlayerCharacter;
import engine.objects.Runegate;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/*
 * This class contains all methods necessary to drive periodic
 * updates of the game simulation from the main _exec loop.
 */
public enum SimulationManager {

	SERVERHEARTBEAT;

	private static SimulationManager instance = null;

	private static final long CITY_PULSE = 2000;
	private static final long RUNEGATE_PULSE = 3000;
	private static final long UPDATE_PULSE = 1000;
	private static final long FlIGHT_PULSE = 100;

	private long _cityPulseTime = System.currentTimeMillis() + CITY_PULSE;
	private long _runegatePulseTime = System.currentTimeMillis()
			+ RUNEGATE_PULSE;
	private long _updatePulseTime = System.currentTimeMillis() + UPDATE_PULSE;
	private long _flightPulseTime = System.currentTimeMillis() + FlIGHT_PULSE;

	public static Duration executionTime = Duration.ofNanos(1);
	public static Duration executionMax = Duration.ofNanos(1);

	private SimulationManager() {

		// don't allow instantiation.
	}

	public static String getPopulationString() {

		String popString = "";

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement getPopString = connection.prepareStatement("CALL GET_POPULATION_STRING()");) {

			ResultSet rs = getPopString.executeQuery();

			if (rs.next())
				popString = rs.getString("popstring");

		} catch (SQLException e) {
			Logger.error(e.toString());
		}

		return popString;
	}

	/*
	 * Update the simulation. *** Important: Whatever you do in here, do it damn
	 * quick!
	 */
	public void tick() {

		/*
		 * As we're on the main thread we must be sure to catch any possible
		 * errors.
		 *
		 * IF something does occur, disable that particular heartbeat. Better
		 * runegates stop working than the game itself!
		 */
		
		Instant startTime = Instant.now();

		try {
			if ((_flightPulseTime != 0)
					&& (System.currentTimeMillis() > _flightPulseTime))
				pulseFlight();
		} catch (Exception e) {
			Logger.error(
					"Fatal error in City Pulse: DISABLED. Error Message : "
							+ e.getMessage());
		}
		try {

			if ((_updatePulseTime != 0)
					&& (System.currentTimeMillis() > _updatePulseTime))
				pulseUpdate();
		} catch (Exception e) {
			Logger.error(
					"Fatal error in Update Pulse: DISABLED");
			//  _runegatePulseTime = 0;
		}

		try {
			if ((_runegatePulseTime != 0)
					&& (System.currentTimeMillis() > _runegatePulseTime))
				pulseRunegates();
		} catch (Exception e) {
			Logger.error(
					"Fatal error in Runegate Pulse: DISABLED");
			_runegatePulseTime = 0;
		}

		try {
			if ((_cityPulseTime != 0)
					&& (System.currentTimeMillis() > _cityPulseTime))
				pulseCities();
		} catch (Exception e) {
			Logger.error(
					"Fatal error in City Pulse: DISABLED. Error Message : "
							+ e.getMessage());
			e.printStackTrace();
	
		}

		SimulationManager.executionTime = Duration.between(startTime, Instant.now());

		if (executionTime.compareTo(executionMax) > 0)
			executionMax = executionTime;
	}

	/*
	 * Mainline simulation update method: handles movement and regen for all
	 * player characters
	 */

	private void pulseUpdate() {

		Collection<AbstractGameObject> playerList;

		playerList = DbManager.getList(GameObjectType.PlayerCharacter);

		// Call update() on each player in game

		if (playerList == null)
			return;

		for (AbstractGameObject ago : playerList) {
			PlayerCharacter player = (PlayerCharacter)ago;

			if (player == null)
				continue;
			player.update();
		}

        _updatePulseTime = System.currentTimeMillis() + 500;
	}
	
	private void pulseFlight() {

		Collection<AbstractGameObject> playerList;

		playerList = DbManager.getList(GameObjectType.PlayerCharacter);

		// Call update() on each player in game

		if (playerList == null)
			return;

		for (AbstractGameObject ago : playerList) {
			PlayerCharacter player = (PlayerCharacter)ago;

			if (player == null)
				continue;
			
			
			player.updateFlight();
		}

        _flightPulseTime = System.currentTimeMillis() + FlIGHT_PULSE;
	}

	private void pulseCities() {

		City city;

		// *** Refactor: Need a list cached somewhere as it doesn't change very
		// often at all.  Have a cityListIsDirty boolean that gets set if it
		// needs an update.  Will speed up this method a great deal.

		Collection<AbstractGameObject> cityList = DbManager.getList(Enum.GameObjectType.City);

		if (cityList == null) {
			Logger.info( "City List null");
			return;
		}

		for (AbstractGameObject cityObject : cityList) {
			city = (City) cityObject;
				city.onEnter();
		}

		_cityPulseTime = System.currentTimeMillis() + CITY_PULSE;
	}

	/*
	 * Method runs proximity collision detection for all active portals on the
	 * game's Runegates
	 */
	private void pulseRunegates() {

		for (Runegate runegate : Runegate._runegates.values()) {
			runegate.collidePortals();
		}

		_runegatePulseTime = System.currentTimeMillis() + RUNEGATE_PULSE;

	}
}
