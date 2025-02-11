package engine.AiPlayers;

import engine.objects.City;
import engine.objects.Mob;
import engine.objects.Race;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AiPlayerManager {

    public static ArrayList<AiPlayer> AiPlayers = new ArrayList<>();
    public static int totalPlayers = 100;

    private static final List<String> GENDER_NEUTRAL_NAMES = Arrays.asList(
            "Alex", "Andy", "Avery", "Bailey", "Blake", "Cameron", "Casey", "Charlie", "Dakota", "Dallas",
            "Devin", "Drew", "Elliot", "Emerson", "Finley", "Frankie", "Gray", "Harley", "Hayden", "Hunter",
            "Jackie", "Jamie", "Jay", "Jessie", "Jordan", "Jules", "Kai", "Keegan", "Kendall", "Lane",
            "Leighton", "Lennon", "Lennox", "Logan", "Mackenzie", "Marley", "Mason", "Micah", "Morgan", "Nico",
            "Noel", "Oakley", "Parker", "Payton", "Phoenix", "Quinn", "Reagan", "Reese", "Remy", "Riley",
            "River", "Robin", "Rowan", "Rory", "Ryan", "Sage", "Sam", "Sawyer", "Shay", "Shiloh",
            "Sky", "Skyler", "Spencer", "Stevie", "Sydney", "Tatum", "Taylor", "Toby", "Toni", "Tyler",
            "Val", "Wesley", "Winter", "Zephyr", "Arden", "Aspen", "Blaine", "Briar", "Brook", "Camdyn",
            "Chandler", "Corey", "Denver", "Devon", "Eden", "Ellis", "Emory", "Ever", "Everest", "Fallon",
            "Flynn", "Indigo", "Justice", "Karter", "Kyrie", "Lex", "Lyric", "Monroe", "Peyton", "Sterling"
    );

    private static final int[] hamletIds = {36105, 36245, 36423, 36562, 36661, 39049};

    private static final Random RANDOM = new Random();

    public static void init(){
        while(AiPlayers.size() < totalPlayers){
            try {
                AiPlayer aiPlayer = new AiPlayer();
                if (aiPlayer != null) {
                    if (aiPlayer.emulated != null) {
                        AiPlayers.add(aiPlayer);
                    }
                }
            }catch(Exception e){
                Logger.error(e);
            }
        }
    }
    public static String generateFirstName(){
        return GENDER_NEUTRAL_NAMES.get(RANDOM.nextInt(GENDER_NEUTRAL_NAMES.size()));
    }

    public static Race getRandomRace(){
        int RaceId = ThreadLocalRandom.current().nextInt(1999,2029);
        while (RaceId == 2020 || RaceId == 2021 || RaceId == 2018 || RaceId == 2019){
            RaceId = ThreadLocalRandom.current().nextInt(1999,2029);
        }
        Race race = Race.getRace(RaceId);
        return race;
    }

    public static City getRandomHamlet() {
        return City.getCity(hamletIds[RANDOM.nextInt(hamletIds.length)]);
    }

    public static void runAi(Mob mob){

    }

}
