import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.List;

class Car{
    private double posX;
    private double posY;
    private double horizontalBorder;
    private double verticalBorder;
    private double speed;
    private Canvas canvas;
    private Image image;
    static double CAR_HEIGHT = 220.0;
    static double CAR_WIDTH = 120.0;

    public Car(double x, double y, double m, Canvas c, Image i){
        posX = x;
        posY = y;
        horizontalBorder = c.getWidth();
        verticalBorder = c.getHeight();
        speed = m;
        canvas = c;
        image = i;
    }

    private void moveTop(){
        if(posY - speed >= 0){
            clear();
            posY -= speed;
            display();
        }
    }

    public void moveBottom(){
            clear();
            posY += speed;
            display();
    }

    public void moveLeft(){
        if(posX - speed >= 0){
            clear();
            posX -= speed;
            display();
        }
    }

    public void moveRight(){
        if(posX + speed <= horizontalBorder){
            clear();
            posX += speed;
            display();
        }
    }

    public void setSpeed(double speed){
        this.speed = speed;
    }

    public double getPosX(){
        return posX;
    }

    public double getPosY(){
        return posY;
    }

    public void setPosX(double posX){
        this.posX = posX;
    }

    public void setPosY(double posY){
        this.posY = posY;
    }

    private void clear(){
        canvas.getGraphicsContext2D().clearRect(posX, posY, CAR_WIDTH, CAR_HEIGHT);
    }

    public void display(){
        canvas.getGraphicsContext2D().drawImage(image, posX, posY, CAR_WIDTH, CAR_HEIGHT);
    }
}

class KeyPressed implements EventHandler<KeyEvent>{
    private Road road;

    public KeyPressed(Road road){
        this.road = road;
    }

    @Override
    public void handle(KeyEvent event){
        road.update(event.getCode());
    }
}

class KeyReleased implements EventHandler<KeyEvent>{
    private Road road;

    public KeyReleased(Road road){
        this.road = road;
    }

    @Override
    public void handle(KeyEvent event){
        if(event.getCode().compareTo(KeyCode.W) == 0 || event.getCode().compareTo(KeyCode.S) == 0){
            road.speedToNormal();
        }
    }
}

class Road extends Pane{
    private Canvas canvas, roadLayer, carsLayer;
    private List<Car> oncomingCars, passingCars;
    private Car car;
    private Timeline normalSpeed, lowSpeed, highSpeed, endGame;
    private int roadMoving;
    private final int carsCounter = 6;
    private final int POSITION = 2;
    private final double HEIGHT = 700;
    private final double WIDTH = 900;
    private final double CAR_BIAS = (WIDTH/4.0 - Car.CAR_WIDTH)/2.0;
    private final double BORDER = 20;
    private Random random;

    Road(Canvas canvas){
        this.canvas = canvas;
        Image image = new Image("/resources/pictures/car.png");
        this.car = new Car(canvas.getWidth()/2.0 + CAR_BIAS, canvas.getHeight() - Car.CAR_HEIGHT - CAR_BIAS/2.0, 10, canvas, image);
        oncomingCars = new ArrayList<>(carsCounter/2);
        passingCars = new ArrayList<>(carsCounter/2);
        roadMoving = 0;
        AudioClip audioClip = new AudioClip(this.getClass().getResource("/resources/sound/sound.mp3").toString());
        audioClip.setVolume(0.2);
        audioClip.setCycleCount(AudioClip.INDEFINITE);
        audioClip.play();
        createRoadLayer();
        createCarsLayer();
        createTimeLine();
        this.getChildren().add(canvas);
        canvas.toFront();
    }

    private void createRoadLayer(){
        roadLayer = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = roadLayer.getGraphicsContext2D();
        gc.setLineWidth(5);
        gc.setFill(Color.BLACK);
        gc.strokeRect(canvas.getWidth()/2.0 - 5, -5, 10, canvas.getHeight()+5);
        this.getChildren().add(roadLayer);
    }

    private void createCarsLayer(){
        carsLayer = new Canvas(WIDTH, HEIGHT);
        random = new Random();
        for(int i = 0; i < carsCounter/2; i++){
            Image image = new Image("/resources/pictures/cars_mini_" + (i + 1) + ".png");
            oncomingCars.add(new Car(random.nextInt(POSITION)*canvas.getWidth()/4.0 + CAR_BIAS, i*-Car.CAR_HEIGHT - Car.CAR_HEIGHT - BORDER*i, 7, carsLayer, image));
        }
        for(int i = 0; i < carsCounter/2; i++){
            Image image = new Image("/resources/pictures/cars_mini_" + (i + 4) + ".png");
            passingCars.add(new Car((random.nextInt(POSITION)+2)*canvas.getWidth()/4.0 + CAR_BIAS, i*-Car.CAR_HEIGHT - Car.CAR_HEIGHT - BORDER*i,3, carsLayer, image));
        }
        this.getChildren().add(carsLayer);
    }

    private void createTimeLine(){
        EventHandler<ActionEvent> eventHandler = event -> {
            GraphicsContext gc = roadLayer.getGraphicsContext2D();
            gc.clearRect(canvas.getWidth() / 4.0 - 5, 0, 10, HEIGHT);
            gc.clearRect(canvas.getWidth() / 4.0 * 3.0 - 5, 0, 10, HEIGHT);
            for(int i = 0; i < 25; i++){
                if(roadMoving == 5) roadMoving = 0;
                else roadMoving++;
                double q = 15;
                if(roadMoving == 5) q += q;
                else q -= q;
                double x = canvas.getWidth();
                double y = canvas.getHeight();
                gc.strokeLine(x / 4.0, i * y / 25 + q, x / 4.0, (i + 0.7) * y / 25 + q);
                gc.strokeLine(x / 4.0 * 3.0, i * y / 25 + q, x / 4.0 * 3.0, (i + 0.7) * y / 25 + q);
            }
            for(int i = 0; i < oncomingCars.size(); i++){
                Car bot = oncomingCars.get(i);
                bot.moveBottom();
                if(checkCrash(bot, car)){
                    showCrash();
                }
                if(bot.getPosY() >= carsLayer.getHeight()){
                    ArrayList<Double> x = new ArrayList<>(carsCounter/2-1);
                    ArrayList<Double> y = new ArrayList<>(carsCounter/2-1);
                    for(int k = 0; k < oncomingCars.size(); k++){
                        if(i != k){
                            x.add(oncomingCars.get(k).getPosX());
                            y.add(oncomingCars.get(k).getPosY());
                        }
                    }
                    while(true){
                        double newX = random.nextInt(POSITION) * canvas.getWidth() / 4.0 + CAR_BIAS;
                        double newY = random.nextInt(POSITION + 2) * -Car.CAR_HEIGHT - Car.CAR_HEIGHT - BORDER*i;
                        if((Math.abs(x.get(0) - newX) != 0 || Math.abs(y.get(0) - newY) >= Car.CAR_HEIGHT + BORDER) && (Math.abs(x.get(1) - newX) != 0 || Math.abs(y.get(1) - newY) >= Car.CAR_HEIGHT + BORDER)){
                            bot.setPosX(newX);
                            bot.setPosY(newY);
                            break;
                        }
                    }
                }
            }
            for(int i = 0; i < passingCars.size(); i++){
                Car bot = passingCars.get(i);
                bot.moveBottom();
                if(checkCrash(bot, car)){
                    showCrash();
                }
                if(bot.getPosY() >= carsLayer.getHeight()){
                    ArrayList<Double> x = new ArrayList<>(carsCounter/2-1);
                    ArrayList<Double> y = new ArrayList<>(carsCounter/2-1);
                    for(int k = 0; k < passingCars.size(); k++){
                        if(i != k){
                            x.add(passingCars.get(k).getPosX());
                            y.add(passingCars.get(k).getPosY());
                        }
                    }
                    while(true){
                        double newX = (random.nextInt(POSITION) + 2) * canvas.getWidth() / 4.0 + CAR_BIAS;
                        double newY = random.nextInt(POSITION + 2) * -Car.CAR_HEIGHT - Car.CAR_HEIGHT - BORDER*i;
                        if((Math.abs(x.get(0) - newX) != 0 || Math.abs(y.get(0) - newY) >= Car.CAR_HEIGHT + BORDER) && (Math.abs(x.get(1) - newX) != 0 || Math.abs(y.get(1) - newY) >= Car.CAR_HEIGHT + BORDER)){
                            bot.setPosX(newX);
                            bot.setPosY(newY);
                            break;
                        }
                    }
                }
            }
        };
        lowSpeed = new Timeline(new KeyFrame(Duration.millis(30), eventHandler));
        normalSpeed = new Timeline(new KeyFrame(Duration.millis(20), eventHandler));
        highSpeed = new Timeline(new KeyFrame(Duration.millis(10), eventHandler));
        lowSpeed.setCycleCount(Timeline.INDEFINITE);
        normalSpeed.setCycleCount(Timeline.INDEFINITE);
        highSpeed.setCycleCount(Timeline.INDEFINITE);
    }

    private boolean checkCrash(Car a, Car b){
        double aX = a.getPosX();
        double aY = a.getPosY() > 0 ? a.getPosY() : 0;
        double bX = b.getPosX();
        double bY = b.getPosY() > 0 ? b.getPosY() : 0;
        return Math.abs(aX - bX) < Car.CAR_WIDTH - BORDER && Math.abs(aY - bY) < Car.CAR_HEIGHT - BORDER;
    }

    private void showCrash(){
        lowSpeed.stop();
        lowSpeed.setCycleCount(0);
        normalSpeed.stop();
        normalSpeed.setCycleCount(0);
        highSpeed.stop();
        highSpeed.setCycleCount(0);
        car.setSpeed(0);
        Car police = new Car(car.getPosX() + Car.CAR_WIDTH + BORDER, -Car.CAR_HEIGHT, 10, canvas, new Image("/resources/pictures/police_car.png"));
        final int[] buzzer = {0};
        canvas.getGraphicsContext2D().setFill(Color.BLUE);
        endGame = new Timeline(new KeyFrame(Duration.millis(20), ae->{
            if(Math.abs(police.getPosY() - car.getPosY()) >= BORDER){
                police.moveBottom();
                buzzer[0]++;
                if(buzzer[0] == 5){
                    canvas.getGraphicsContext2D().fillRect(police.getPosX() + Car.CAR_WIDTH / 2 - 30, police.getPosY() - 50 + Car.CAR_HEIGHT / 2, 15, 20);
                }
                else if(buzzer[0] == 10){
                    canvas.getGraphicsContext2D().fillRect(police.getPosX() + Car.CAR_WIDTH / 2 + 15, police.getPosY() - 50 + Car.CAR_HEIGHT / 2, 15, 20);
                    buzzer[0] = 0;
                }

            } else {
                endGame.stop();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("");
                alert.setTitle("End Game");
                alert.setContentText("Crash");
                alert.show();
            }
        }));
        endGame.setCycleCount(Timeline.INDEFINITE);
        endGame.play();
    }

    public void update(KeyCode keyCode){
        switch(keyCode){
            case W:{
                lowSpeed.stop();
                normalSpeed.stop();
                highSpeed.play();
                break;
            }
            case S:{
                normalSpeed.stop();
                highSpeed.stop();
                lowSpeed.play();
                for(Car bot : passingCars){
                    bot.setSpeed(0);
                }
                break;
            }
            case A:{
                car.moveLeft();
                break;
            }
            case D:{
                car.moveRight();
                break;
            }
        }
    }

    public void speedToNormal(){
        lowSpeed.stop();
        highSpeed.stop();
        normalSpeed.play();
        for(Car bot : passingCars){
            bot.setSpeed(3);
        }
    }

    public void start(){
        car.display();
        normalSpeed.play();
    }
}

public class Main extends Application{
    final double HEIGHT = 700;
    final double WIDTH = 900;

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Lab 8");
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        Road road = new Road(canvas);
        road.start();
        root.setCenter(road);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, new KeyPressed(road));
        scene.addEventHandler(KeyEvent.KEY_RELEASED, new KeyReleased(road));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
