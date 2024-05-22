package CyberClaw;
import robocode.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Bootron extends AdvancedRobot { 					 // Coeficientes e intercepto
    private static final double[] coeficientes = {0.01, -0.01, 0.02, 0.01, -0.02}; 
    private static final double intercepto = 0.5;
    private static final double margemParede = 60;

    private List<Double> distancias = new ArrayList<Double>();
    private List<Double> angulos = new ArrayList<Double>();
    private List<Double> velocidades = new ArrayList<Double>();
    private List<Double> roboDireçoes = new ArrayList<Double>();
    private List<Double> inimigoDireçoes = new ArrayList<Double>();

    public void run() {
       setBodyColor(new Color(0, 0, 0));
	   setGunColor(new Color(255, 215, 0));
	   setRadarColor(new Color(255, 0, 0));
	   setBulletColor(new Color(255, 255, 0));
	   setScanColor(new Color(75, 0, 130));


        while (true) {
            double[] features = getFeatures();
            double probabilidade = predictMovement(features);

            if (probabilidade > 0.5) {
                moveSafely(50);
            } else {
                moveSafely(-50);
            }

            setTurnRadarRight(360); 
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {   		// Regressão Logística na Decisão de Movimento
        double distancia = e.getDistance();
        double angulo = e.getBearing();
        double velocidade = e.getVelocity();
        double inimigoDireçao = e.getHeading();
        double energia = e.getEnergy();
        
        System.out.println("Inimigo detectado:");
        System.out.println("Distância: " + distancia);
        System.out.println("Direção: " + angulo);
        System.out.println("Energia: " + energia);
        System.out.println("Velocidade: " + velocidade);
        System.out.println("Ângulo: " + inimigoDireçao); 

        distancias.add(distancia);
        angulos.add(angulo);
        velocidades.add(velocidade);
        inimigoDireçoes.add(inimigoDireçao);

        if (distancia < 100) {
            double gunTurn = getHeading() + angulo - getGunHeading();
            turnGunRight(normalizeBearing(gunTurn));
        } else {
            double tempo = distancia / (20 - 3 * firePower(distancia));
            double futuraPosicaoX = getX() + distancia * Math.sin(Math.toRadians(getHeading() + angulo)) + Math.sin(Math.toRadians(inimigoDireçao)) * velocidade * tempo;
            double futuraPosicaoY = getY() + distancia * Math.cos(Math.toRadians(getHeading() + angulo)) + Math.cos(Math.toRadians(inimigoDireçao)) * velocidade * tempo;
            double anguloParaAtirar = Math.toDegrees(Math.atan2(futuraPosicaoX - getX(), futuraPosicaoY - getY()));
            turnGunRight(normalizeBearing(anguloParaAtirar - getGunHeading()));
        }

        fire(firePower(distancia));
        if (distancias.size() > 100) {
            distancias.remove(0);
            angulos.remove(0);
            velocidades.remove(0); 
            roboDireçoes.remove(0);
            inimigoDireçoes.remove(0);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        setBack(50);
        setTurnRight(30);
        execute();
    }

    public void onHitWall(HitWallEvent e) {
        setBack(20);
        setTurnRight(90);
        setAhead(15);
        setTurnRight(90);
        execute();
    }

    double normalizeBearing(double angulo) {
        while (angulo > 180) angulo -= 360;
        while (angulo < -180) angulo += 360;
        return angulo;
    }

    private double sigmoid(double z) { 			// Função Sigmoide
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double dotProduct(double[] a, double[] b) {		// Produto Escalar
        double resultado = 0.0;
        for (int i = 0; i < a.length; i++) {
            resultado += a[i] * b[i];
        }
        return resultado;
    }

    private double predictMovement(double[] features) {		// Previsão de movimento
        double z = dotProduct(features, coeficientes) + intercepto;
        return sigmoid(z);
    }

    private double[] getFeatures() {		// Extraindo as características do robô inimigo
        double ultimaDistancia = distancias.isEmpty() ? 0 : distancias.get(distancias.size() - 1);
        double ultimoAngulo = angulos.isEmpty() ? 0 : angulos.get(angulos.size() - 1);
        double ultimaVelocidade = velocidades.isEmpty() ? 0 : velocidades.get(velocidades.size() - 1);
        double ultimaDireçaoRobo = roboDireçoes.isEmpty() ? 0 : roboDireçoes.get(roboDireçoes.size() - 1);
        double ultimaDireçaoInimigo = inimigoDireçoes.isEmpty() ? 0 : inimigoDireçoes.get(inimigoDireçoes.size() - 1);

        return new double[]{ultimaDistancia, ultimoAngulo, ultimaVelocidade, ultimaDireçaoRobo, ultimaDireçaoInimigo};
    }

    private void moveSafely(double distancia) {
        double futureX = getX() + Math.sin(Math.toRadians(getHeading())) * distancia;
        double futureY = getY() + Math.cos(Math.toRadians(getHeading())) * distancia;
        if (futureX > margemParede && futureX < getBattleFieldWidth() - margemParede &&
            futureY > margemParede && futureY < getBattleFieldHeight() - margemParede) {
            setAhead(distancia);
        } else {
            setTurnRight(90);
            setAhead(50);
        }
    }

    private double firePower(double distancia) {
        if (distancia <= 60) {
            return 3;
        } else if (distancia <= 500) {
            return 2;
        } else {
            return 1;
        }
    }
}