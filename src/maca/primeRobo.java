package maca;
import robocode.*;
import robocode.util.Utils;
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


        while (true) {
            if (getRadarTurnRemaining() == 0) setTurnRadarRight(360);
            if (random.nextInt(20) == 10) isMovingFw = !isMovingFw;
            if (random.nextInt(30) == 10) setTurnRight(random.nextInt(110) - 45);
            setAhead(isMovingFw ? 100 : -100);
            avoidWalls();
            execute();
        }

    }

    private void randomizeMovement (ScannedRobotEvent e) {
        // try to keep an ideal distance of 200 from the enemy
        double idealDistance = 200;
        double distanceError = e.getDistance() - idealDistance;
        // Only changing the movement in case of a relevant distanceError
        if (Math.abs(distanceError) > 20) {
            // Too far, tries to close in the distance
            if (distanceError > 0) {
                setTurnRight(e.getBearing());
                setAhead(distanceError * 0.33);
            }
            // Too close, tries to move away
            else {
                setTurnRight(e.getBearing() - 180);
                setAhead(-distanceError * 0.33);
            }
        }
        else{
            // keeps perpendicular movements around the range
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 90));
            if (isMovingFw) setAhead(100);
            else setAhead(-100);
            if (random.nextInt(20) == 7) isMovingFw = !isMovingFw;
        }
    }

    private void avoidWalls () {
        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        double x = getX();
        double y = getY();
        double buffer = PERCENT_SUBSQUARE * Math.max(width, height);

        // The angles are given as a compass, as in:
        // 0: North
        // 90: East
        // 180: South
        // 270: West

        // Closing in the bottom wall
        if (y < buffer) {
            if (getHeading() >= 180 && getHeading() <= 270) setTurnRight(90);
            else if (getHeading() >= 90  && getHeading() < 180 ) setTurnLeft(90);
        }
        // Closing in the top wall
        else if (y > height - buffer) {
            if (getHeading() >= 0 && getHeading() <= 90) setTurnRight(90);
            else if (getHeading() >= 270 && getHeading() < 360) setTurnLeft(90);
        }
        // Closing in the left wall
        else if (x < buffer) {
            if (getHeading() >= 270 && getHeading() <= 360) setTurnRight(90);
            else if (getHeading() >= 180 && getHeading() < 270) setTurnLeft(90);
        }
        // Closing in the right wall
        else if (x >= width - buffer) {
            if (getHeading() >= 90 && getHeading() <= 180) setTurnRight(90);
            else if (getHeading() >= 0 && getHeading() < 90)  setTurnLeft(90);
        }
        setAhead(100);
        isMovingFw = true;
    }

    public void onHitWall(HitWallEvent e) {
        // just turn directions
        setTurnRight(Utils.normalRelativeAngleDegrees(180 - e.getBearing()));
        isMovingFw = !isMovingFw;
        setAhead(isMovingFw ? 150 : -150);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // changes direction and dodges it perpendicular to the bullet
        // direction to make it harder to hit
        double bulletBearing = e.getBearing();
        setTurnRight(Utils.normalRelativeAngleDegrees(90 - bulletBearing));
        isMovingFw = !isMovingFw;
        setAhead(isMovingFw ? 150 : -150);
    }

    public void onHitRobot(HitRobotEvent e) {
        if (e.isMyFault()){
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 180));
            setTurnGunRight(Utils.normalRelativeAngleDegrees(getHeading() + e.getBearing() - getGunHeading()));
            if (getGunHeat() == 0) setFire(3);
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() +90));
            setAhead(100);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        foundEnemy = true;
        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
        setTurnRadarRight(radarTurn * 2);
        enemyDist = e.getDistance();

        randomizeMovement (e);
        aimAndShooting(e);
    }

    private void aimAndShooting (ScannedRobotEvent e) {
        double eHeading = e.getHeading();
        double eVelocity = e.getVelocity();
        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();

        while (radarTurn > 180) radarTurn -= 360;
        while (radarTurn < -180) radarTurn += 360;

        double absBearing = getHeading() + e.getBearing();
        double eX = getX() + Math.sin(Math.toRadians(absBearing)) * enemyDist;
        double eY = getY() + Math.cos(Math.toRadians(absBearing)) * enemyDist;

        double firePower = Math.min(3, Math.max(0.1, 400 / enemyDist));
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