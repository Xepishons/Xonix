import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

class GameXonix extends JFrame {

    final String TITLE_OF_PROGRAM = "Xonix";
    final int POINT_SIZE = 10; //размер точки
    final int FIELD_WIDTH = 640 / POINT_SIZE; //размер поля
    final int FIELD_HEIGHT = 460 / POINT_SIZE;
    final int FIELD_DX = 6;
    final int FIELD_DY = 56;
    final int START_LOCATION = 200; //координата верхнего левого угла когда окно откроется
    final int LEFT = 37; // Коды клавиш
    final int UP = 38;
    final int RIGHT = 39;
    final int DOWN = 40;
    final int SHOW_DELAY = 60; // задержка для анимации
    final int COLOR_TEMP = 1;
    final int COLOR_WATER = 0; //цвет воды
    final int COLOR_LAND = 0x00FF00; //цвет земли  / 0x00a8a8
    final int COLOR_TRACK = 0xFF0000; //цвет следа / 0x901290
    final int PERCENT_OF_WATER_CAPTURE = 70; //процент перехода на следующий уровень
    final String FORMAT_STRING = "Score: %d %20s %d %20s %2.0f%%";
    final Font font = new Font("", Font.BOLD, 21); //размер шрифта?
    Random random = new Random(); //объекты
    Canvas canvas = new Canvas(); //отображение поля
    JLabel board = new JLabel();
    Delay delay = new Delay();
    Field field = new Field();
    Xonix xonix = new Xonix();
    Balls balls = new Balls();
    Cube cube = new Cube();
    GameOver gameover = new GameOver();

    public static void main(String[] args) {
        new GameXonix().go();
    }

    GameXonix() { //конструктор
        setTitle(TITLE_OF_PROGRAM); //имя заголовка программы
        setDefaultCloseOperation(EXIT_ON_CLOSE); //условия закрытия программы (нажатие на крестик)
        setBounds(START_LOCATION, START_LOCATION, FIELD_WIDTH*POINT_SIZE + FIELD_DX, FIELD_HEIGHT*POINT_SIZE + FIELD_DY); //размер окна
        setResizable(false); //не будем изменять размер
        board.setFont(font);//нижняя панель
        board.setOpaque(true); //выключает прозрачность метки
        board.setBackground(Color.black);
        board.setForeground(Color.white);
        board.setHorizontalAlignment(JLabel.CENTER);
        add(BorderLayout.CENTER, canvas);
        add(BorderLayout.SOUTH, board);
        addKeyListener(new KeyAdapter() { //прослушатель клавиш
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() >= LEFT && e.getKeyCode() <= DOWN)
                    xonix.setDirection(e.getKeyCode());
            }
        });
        setVisible(true);
    }

    void go() { //механика игры
        while (!gameover.isGameOver()) {
            xonix.move();
            balls.move();
            cube.move();
            canvas.repaint(); //перересовывает картинку на экране
            board.setText(String.format(FORMAT_STRING, field.getCountScore(), "Xn:", xonix.getCountLives(), "Full:", field.getCurrentPercent()));//отобоажение инфы
            delay.wait(SHOW_DELAY); //задержка
            if (xonix.isSelfCrosed() || balls.isHitTrackOrXonix() || cube.isHitXonix()) { //проверка столкновений с собой, шариком, кубиком
                xonix.decreaseCountLives(); //уменьшение жизней
                if (xonix.getCountLives() > 0) {
                    xonix.init();
                    field.clearTrack();
                    delay.wait(SHOW_DELAY * 10);
                }
            }
            if (field.getCurrentPercent() >= PERCENT_OF_WATER_CAPTURE) {  //сколько захваченой территории
                field.init();
                xonix.init(); //инициализация
                cube.init();
                balls.add();
                delay.wait(SHOW_DELAY * 10);
            }
        }
    }

    class Field {
        private final int WATER_AREA = (FIELD_WIDTH - 4)*(FIELD_HEIGHT - 4); //высчитывает площадь воды
        private int[][] field = new int[FIELD_WIDTH][FIELD_HEIGHT]; //цвета
        private float currentWaterArea;
        private int countScore = 0; //очки

        Field() {
            init();
        }

        void init() {
            for (int y = 0; y < FIELD_HEIGHT; y++)
                for (int x = 0; x < FIELD_WIDTH; x++)
                    field[x][y] = (x < 2 || x > FIELD_WIDTH - 3 || y < 2 || y > FIELD_HEIGHT - 3)? COLOR_LAND : COLOR_WATER;
            currentWaterArea = WATER_AREA;
        }

        int getColor(int x, int y) {
            if (x < 0 || y < 0 || x > FIELD_WIDTH - 1 || y > FIELD_HEIGHT - 1) return COLOR_WATER;
            return field[x][y];
        }

        void setColor(int x, int y, int color) { field[x][y] = color; } //записывает цвет

        int getCountScore() { return countScore; }
        float getCurrentPercent() { return 100f - currentWaterArea / WATER_AREA * 100; }

        void clearTrack() { // стирает пути ксоника
            for (int y = 0; y < FIELD_HEIGHT; y++)
                for (int x = 0; x < FIELD_WIDTH; x++)
                    if (field[x][y] == COLOR_TRACK) field[x][y] = COLOR_WATER;
        }

        void fillTemporary(int x, int y) { // временное заполнение зоны
            if (field[x][y] > COLOR_WATER) return;
            field[x][y] = COLOR_TEMP; // filling temporary color
            for (int dx = -1; dx < 2; dx++)
                for (int dy = -1; dy < 2; dy++) fillTemporary(x + dx, y + dy);
        }

        void tryToFill() { //рекурсивный метод / проблемы с закраской отрезаных кусоков
            currentWaterArea = 0;
            for (Ball ball : balls.getBalls()) fillTemporary(ball.getX(), ball.getY()); //
            for (int y = 0; y < FIELD_HEIGHT; y++)
                for (int x = 0; x < FIELD_WIDTH; x++) {
                    if (field[x][y] == COLOR_TRACK || field[x][y] == COLOR_WATER) {
                        field[x][y] = COLOR_LAND;
                        countScore += 10;
                    }
                    if (field[x][y] == COLOR_TEMP) {
                        field[x][y] = COLOR_WATER;
                        currentWaterArea++;
                    }
                }
        }

        void paint(Graphics g) {
            for (int y = 0; y < FIELD_HEIGHT; y++)
                for (int x = 0; x < FIELD_WIDTH; x++) {
                    g.setColor(new Color(field[x][y]));
                    g.fillRect(x*POINT_SIZE, y*POINT_SIZE, POINT_SIZE, POINT_SIZE);
                }
        }
    }

    class Xonix {
        private int x, y, direction, countLives = 3;
        private boolean isWater, isSelfCross; //нахождение на воде / пересекание себя

        Xonix() {
            init();
        }

        void init() { // установлени ксоника в определённую точку
            y = 0;
            x = FIELD_WIDTH / 2;
            direction = 0;
            isWater = false;
        }

        int getX() { return x; }
        int getY() { return y; }
        int getCountLives() { return countLives; }

        void decreaseCountLives() { countLives--; }

        void setDirection(int direction) { this.direction = direction; } //меняет направление ксоника

        void move() {
            if (direction == LEFT) x--;
            if (direction == RIGHT) x++;
            if (direction == UP) y--;
            if (direction == DOWN) y++;
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if (y > FIELD_HEIGHT - 1) y = FIELD_HEIGHT - 1;
            if (x > FIELD_WIDTH - 1) x = FIELD_WIDTH - 1;
            isSelfCross = field.getColor(x, y) == COLOR_TRACK; //налетел ли наслед свой
            if (field.getColor(x, y) == COLOR_LAND && isWater) { //если ксоние с воды перешел наземлю
                direction = 0;
                isWater = false;
                field.tryToFill();
            }
            if (field.getColor(x, y) == COLOR_WATER) {
                isWater = true;
                field.setColor(x, y, COLOR_TRACK);
            }
        }

        boolean isSelfCrosed() { return isSelfCross; }

        void paint(Graphics g) {
            g.setColor((field.getColor(x, y) == COLOR_LAND) ? new Color(COLOR_TRACK) : Color.white);
            g.fillRect(x*POINT_SIZE, y*POINT_SIZE, POINT_SIZE, POINT_SIZE);
            g.setColor((field.getColor(x, y) == COLOR_LAND) ? Color.white : new Color(COLOR_TRACK));
            g.fillRect(x*POINT_SIZE + 3, y*POINT_SIZE + 3, POINT_SIZE - 6, POINT_SIZE - 6);
        }
    }

    class Balls {
        private ArrayList<Ball> balls = new ArrayList<Ball>();

        Balls() {
            add();
        }

        void add() { balls.add(new Ball()); } //добавление шарика

        void move() { for (Ball ball : balls) ball.move(); } //перемещение шариков/ foreach

        ArrayList<Ball> getBalls() { return balls; }

        boolean isHitTrackOrXonix() { //
            for (Ball ball : balls) if (ball.isHitTrackOrXonix()) return true;
            return false;
        }

        void paint(Graphics g) { for (Ball ball : balls) ball.paint(g); }
    }

    class Ball {
        private int x, y, dx, dy; //dx - смещение

        Ball() {
            do { //проверка места объекта в поле
                x = random.nextInt(FIELD_WIDTH);
                y = random.nextInt(FIELD_HEIGHT);
            } while (field.getColor(x, y) > COLOR_WATER);
            dx = random.nextBoolean()? 1 : -1;
            dy = random.nextBoolean()? 1 : -1;
        }

        void updateDXandDY() {//изменение
            if (field.getColor(x + dx, y) == COLOR_LAND) dx = -dx;
            if (field.getColor(x, y + dy) == COLOR_LAND) dy = -dy;
        }

        void move() { //изменение координат
            updateDXandDY();
            x += dx;
            y += dy;
        }

        int getX() { return x; }
        int getY() { return y; }

        boolean isHitTrackOrXonix() {//столкновение
            updateDXandDY();
            if (field.getColor(x + dx, y + dy) == COLOR_TRACK) return true;
            if (x + dx == xonix.getX() && y + dy == xonix.getY()) return true;
            return false;
        }

        void paint(Graphics g) {
            g.setColor(Color.white);
            g.fillOval(x*POINT_SIZE, y*POINT_SIZE, POINT_SIZE, POINT_SIZE);
            g.setColor(new Color(COLOR_LAND));
            g.fillOval(x*POINT_SIZE + 2, y*POINT_SIZE + 2, POINT_SIZE - 4, POINT_SIZE - 4);
        }
    }

    class Cube {
        private int x, y, dx, dy;

        Cube() {
            init();
        }

        void init() { x = dx = dy = 1; }

        void updateDXandDY() {
            if (field.getColor(x + dx, y) == COLOR_WATER) dx = -dx;
            if (field.getColor(x, y + dy) == COLOR_WATER) dy = -dy;
        }

        void move() {
            updateDXandDY();
            x += dx;
            y += dy;
        }

        boolean isHitXonix() {
            updateDXandDY();
            if (x + dx == xonix.getX() && y + dy == xonix.getY()) return true;
            return false;
        }

        void paint(Graphics g) {
            g.setColor(new Color(COLOR_WATER));
            g.fillRect(x*POINT_SIZE, y*POINT_SIZE, POINT_SIZE, POINT_SIZE);
            g.setColor(new Color(COLOR_LAND));
            g.fillRect(x*POINT_SIZE + 2, y*POINT_SIZE + 2, POINT_SIZE - 4, POINT_SIZE - 4);
        }
    }

    class Delay { //задерживает выпонение программы
        void wait(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    class GameOver {
        private final String GAME_OVER_MSG = "GAME OVER";
        private boolean gameOver;

        boolean isGameOver() { return gameOver; }

        void paint(Graphics g) {
            if (xonix.getCountLives() == 0) {
                gameOver = true;
                g.setColor(Color.white);
                g.setFont(new Font("", Font.BOLD, 60));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(GAME_OVER_MSG, (FIELD_WIDTH*POINT_SIZE + FIELD_DX - fm.stringWidth(GAME_OVER_MSG))/2, (FIELD_HEIGHT*POINT_SIZE)/2);
            }
        }
    }

    class Canvas extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            field.paint(g);//отрисовка
            xonix.paint(g);
            balls.paint(g);
            cube.paint(g);
            gameover.paint(g);
        }
    }
}