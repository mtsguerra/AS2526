package maca;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.*;

public class primeRobo extends AdvancedRobot {

    private Random random = new Random();
    private double enemyDist;
    private boolean isMovingFw = true;
    private final double PERCENT_SUBSQUARE = 0.1;
    private boolean foundEnemy = false;

    public void run() {

        // SELECAO BRASILEIRA 2026
        setColors(Color.YELLOW, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.BLUE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            if (!foundEnemy) setTurnRadarRight(360);
            if(!avoidWalls()) setAhead(isMovingFw ? 100 : -100);
            execute();
        }

    }

    /**
     * Tries to keep an ideal distance of 200 from the enemy, only changing
     * the movement in case of a relevant distanceError. In the case of
     * already in the range tries to keep the distance and uses an offset
     * angle to avoid predictive movement such as going back and forth or a
     * perfect circle.
     * @param e current event
     */
    private void randomizeMovement (ScannedRobotEvent e) {
        double idealDistance = 200;
        double distanceError = e.getDistance() - idealDistance;
        if (Math.abs(distanceError) > 20) {
            // Too far, tries to close in the distance
            if (distanceError > 0) {
                setTurnRight(e.getBearing());
                setAhead(distanceError * 0.5);
            }
            // Too close, tries to move away
            else {
                setTurnRight(e.getBearing() - 180);
                setAhead(-distanceError * 0.5);
            }
        }
        else{
            double angleOffset = (random.nextDouble() * 40) - 20;
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 90 +  angleOffset));
            if (isMovingFw) setAhead(150);
            else setAhead(-150);
            if (random.nextInt(100) == 7) isMovingFw = !isMovingFw;
        }
    }

    /**
     * To try avoiding the walls it keeps track of the angle the robot is
     * heading, using the Cartesian Coordinate System as reference, and based
     * off the angle it calculates the direction it must turn to avoid
     * hitting walls. Also handles the special case of getting into a corner.
     * @return is currently avoiding a wall
     */
    private boolean avoidWalls () {
        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        double x = getX();
        double y = getY();
        double buffer = PERCENT_SUBSQUARE * Math.max(width, height);
        boolean avoidingWalls = false;

        boolean nearXWalls = (x < buffer || x >= width - buffer);
        boolean nearYWalls = (y < buffer || y >= height - buffer);
        if (!nearXWalls && !nearYWalls) return false;
        if (nearXWalls && nearYWalls) {
            double angleReativeCenter =
                    Math.toDegrees(Math.atan2((width / 2) - x, (height / 2) - y));
            double turnAngle =
                    Utils.normalRelativeAngleDegrees(angleReativeCenter - getHeading());
            setTurnRight(turnAngle);
            setAhead(150);
            isMovingFw = true;
            return true;
        }

        double realHeading = getHeading();
        if (!isMovingFw) realHeading = (realHeading + 180) % 360;

        // The angles are given as a compass, as in:
        // 0: North
        // 90: East
        // 180: South
        // 270: West

        // Closing in the bottom wall
        if (y < buffer) {
            if (realHeading >= 180 && realHeading <= 270) setTurnRight(90);
            else if (realHeading >= 90  && realHeading < 180 ) setTurnLeft(90);
            avoidingWalls = true;
        }
        // Closing in the top wall
        else if (y > height - buffer) {
            if (realHeading >= 0 && realHeading <= 90) setTurnRight(90);
            else if (realHeading >= 270 && realHeading < 360) setTurnLeft(90);
            avoidingWalls = true;
        }
        // Closing in the left wall
        else if (x < buffer) {
            if (realHeading >= 270 && realHeading <= 360) setTurnRight(90);
            else if (realHeading >= 180 && realHeading < 270) setTurnLeft(90);
            avoidingWalls = true;
        }
        // Closing in the right wall
        else if (x >= width - buffer) {
            if (realHeading >= 90 && realHeading <= 180) setTurnRight(90);
            else if (realHeading >= 0 && realHeading < 90)  setTurnLeft(90);
            avoidingWalls = true;
        }
        if (avoidingWalls) {
            setAhead(100);
            isMovingFw = true;
        }
        return avoidingWalls;
    }

    /**
     * Basic command to turn directions in case of hitting a wall
     * @param e current event
     */
    public void onHitWall(HitWallEvent e) {
        setTurnRight(Utils.normalRelativeAngleDegrees(180 - e.getBearing()));
        isMovingFw = !isMovingFw;
        setAhead(isMovingFw ? 150 : -150);
    }

    /**
     * In case of getting hit by a bullet tries to change direction and dodge
     * it perpendicular to the bullet direction to make it harder to hit
     * @param e current event
     */
    public void onHitByBullet(HitByBulletEvent e) {
        double bulletBearing = e.getBearing();
        setTurnRight(Utils.normalRelativeAngleDegrees(90 - bulletBearing));
        isMovingFw = !isMovingFw;
        setAhead(isMovingFw ? 150 : -150);
    }

    /**
     * If hitting a robot, and it is not the enemy fault, tries to move away
     * from it and keep the aim locked
     * at the robot
     * @param e current event
     */
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
        enemyDist = e.getDistance();

        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
        while (radarTurn > 180) radarTurn -= 360;
        while (radarTurn < -180) radarTurn += 360;
        setTurnRadarRight(radarTurn * 2);

        if (!avoidWalls()) randomizeMovement(e);
        aimAndShooting(e);
    }

    private void aimAndShooting (ScannedRobotEvent e) {
        double eHeading = e.getHeading();
        double eVelocity = e.getVelocity();

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

    /**
     * Just a funny celebration to winning!
     * @param e current event
     */
    public void onWin(WinEvent e) {
        for (int i = 0; i < 100; i++) {
            setTurnRight(360);
            setTurnGunRight(360);
            setTurnRadarLeft(360);
            setAhead(100);
            fire(0.01);
            execute();
        }
    }
}