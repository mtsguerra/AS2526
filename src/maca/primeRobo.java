package maca;
import robocode.*;
import java.util.*;
import java.awt.*;

public class primeRobo extends AdvancedRobot {

    private Random random = new Random();
    private double enemyDist;
    private boolean isMovingFw = true;
    private final double PERCENT_SUBSQUARE = 0.2;

    private boolean foundEnemy = false;

    public void run() {

        // SELECAO BRASILEIRA 2026
        setColors(Color.YELLOW, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.BLUE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            if (!foundEnemy) setTurnRadarRight(360);
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
        foundEnemy = true;
        enemyDist = e.getDistance();
        double eHeading = e.getHeading();
        double eVelocity = e.getVelocity();

        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
        while (radarTurn > 180) radarTurn -= 360;
        while (radarTurn < -180) radarTurn += 360;
        setTurnRadarRight(radarTurn * 2);

        double absBearing = getHeading() + e.getBearing();
        double eX = getX() + Math.sin(Math.toRadians(absBearing)) * enemyDist;
        double eY = getY() + Math.cos(Math.toRadians(absBearing)) * enemyDist;

        double firePower = 1;
        double bulletSpeed = 20 - 3 * firePower;

        double futureX = eX;
        double futureY = eY;
        for (int i = 0; i < 100; i++) {
            double dist = Math.hypot(futureX - getX(), futureY - getY());
            double ticks = dist / bulletSpeed;
            double newFX = eX + Math.sin(Math.toRadians(eHeading)) * eVelocity * ticks;
            double newFY = eY + Math.cos(Math.toRadians(eHeading)) * eVelocity * ticks;
            if (Math.abs(newFX - futureX) < 0.01 && Math.abs(newFY - futureY) < 0.01) break;
            futureX = newFX;
            futureY = newFY;
        }

        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        futureX = Math.max(18, Math.min(width - 18, futureX));
        futureY = Math.max(18, Math.min(height - 18, futureY));

        double dx = futureX - getX();
        double dy = futureY - getY();
        double targetBearing = Math.toDegrees(Math.atan2(dx, dy));
        if (targetBearing < 0) targetBearing += 360;

        double gunTurn = targetBearing - getGunHeading();
        while (gunTurn > 180) gunTurn -= 360;
        while (gunTurn < -180) gunTurn += 360;

        setTurnGunRight(gunTurn);
        setFire(firePower);
    }

    public void onRobotDeath(RobotDeathEvent e) {
        foundEnemy = false;
    }
}