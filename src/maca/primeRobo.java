package maca;
import robocode.*;
import java.util.*;
import java.awt.*;

public class primeRobo extends AdvancedRobot {

    private Random random = new Random();
    private double enemyDist;
    private boolean isMovingFw = true;
    private final double PERCENT_SUBSQUARE = 0.2;

    public void run() {

        // SELECAO BRASILEIRA 2026
        setColors(Color.YELLOW, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.BLUE);


        while (true) {
            turnGunRight(360);
            turnGunRight(360);
            avoidWalls();
            execute();
        }

    }

    private void avoidWalls () {
        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        double buffer = PERCENT_SUBSQUARE * Math.max(width, height);
        double x = getX();
        double y = getY();

        // The angles are given as a compass, as in:
        // 0: North
        // 90: West
        // 180: South
        // 270: East

        // Closing in the bottom wall
        if (y < buffer) {
            if (getHeading() > 90 && getHeading() <= 180) setTurnRight(90);
            else if (getHeading() > 180 && getHeading() < 270) setTurnLeft(90);
        }
        // Closing in the top wall
        else if (y >= height - buffer) {
            if (getHeading() > 270 && getHeading() <= 360) setTurnRight(90);
            else if (getHeading() >= 0 && getHeading() < 90) setTurnLeft(90);
        }
        // Closing in the left wall
        if (x < buffer) {
            if (getHeading() > 0 && getHeading() <= 90) setTurnRight(90);
            else if (getHeading() > 90 && getHeading() < 180) setTurnLeft(90);
        }
        // Closing in the right wall
        else if (x >= width - buffer) {
            if (getHeading() > 180 && getHeading() <= 270) setTurnRight(90);
            else if (getHeading() > 270 && getHeading() < 360)  setTurnLeft(90);
        }
        setAhead(100);
        isMovingFw = true;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        fire(1);
    }
}