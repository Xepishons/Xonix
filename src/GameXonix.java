import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

class GameXonix extends JFrame {

    private final int colorTemp =  1;
    private final int colorWater = 0; //цвет воды
    private final int colorLand = 0x00FF00; //цвет земли
    private final int colorTrack = 0xFF0000; //цвет следа
    private Random random = new Random(); //объекты
    private Canvas canvas = new Canvas(); //отображение поля
    private JLabel board = new JLabel();
    private Delay delay = new Delay();
    private Field field = new Field();
    private Xonix xonix = new Xonix();
    private Balls balls = new Balls();
    private GameOver gameover = new GameOver();

    public static void main(String[] args) {
        new GameXonix().go();
    }

    private GameXonix() { //конструктор
        String title_of_program = "Xonix";
        setTitle(title_of_program); //имя заголовка программы
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //условия закрытия программы (нажатие на крестик)
        setBounds(0, 0, 646, 516); //размер окна
        setResizable(false); //не будем изменять размер
        Font font = new Font("", Font.BOLD, 21);
        board.setFont(font);//нижняя панель
        board.setOpaque(true); //выключает прозрачность метки
        board.setBackground(Color.black);
        board.setForeground(Color.white);
        board.setHorizontalAlignment(JLabel.CENTER);
        add(BorderLayout.CENTER, canvas);
        add(BorderLayout.SOUTH, board);
        addKeyListener(new KeyAdapter() { //прослушатель клавиш
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() >= 37 && e.getKeyCode() <= 40)
                    xonix.setDirection(e.getKeyCode());
            }
        });
        setVisible(true);
    }

    private void go() { //механика игры
        while (!gameover.isGameOver()) {
            xonix.move();
            balls.move();
            canvas.repaint(); //перересовывает картинку на экране
            String formatString = "Score: %d %20s %d %20s %2.0f%%";
            board.setText(String.format(formatString, field.getCountScore(), "Xn:", xonix.getCountLives(), "Full:", field.getCurrentPercent()));//отобоажение информации
            int showDelay = 60; // задержка для анимации
            delay.wait(showDelay); //задержка
            if (xonix.isSelfCrosed() || balls.isHitTrackOrXonix()) { //проверка столкновений с собой, шариком
                xonix.decreaseCountLives(); //уменьшение жизней
                if (xonix.getCountLives() > 0) {
                    xonix.init();
                    field.clearTrack();
                    delay.wait(showDelay * 10); //задержка
                }
            }
            int percentToMove = 70; //процент перехода на следующий уровень
            if (field.getCurrentPercent() >= percentToMove) {  //сколько захваченой территории
                field.init();
                xonix.init(); //инициализация
                balls.add();
                delay.wait(showDelay * 10);
            }
        }
    }

    class Field {
        private final int WATER_AREA = (64 - 4)*(46 - 4); //высчитывает площадь воды
        private int[][] field = new int[64][46]; //цвета
        private float currentWaterArea;
        private int countScore = 0; //очки

        Field() {
            init();
        }

        void init() {
            for (int y = 0; y < 46; y++)
                for (int x = 0; x < 64; x++)
                    field[x][y] = (x < 2 || x > 64 - 3 || y < 2 || y > 46 - 3)? colorLand : colorWater;
            currentWaterArea = WATER_AREA;
        }

        int getColor(int x, int y) {
            if (x < 0 || y < 0 || x > 64 - 1 || y > 46 - 1) return colorWater;
            return field[x][y];
        }

        void setColor(int x, int y, int color) { field[x][y] = color; } //записывает цвет

        int getCountScore() { return countScore; }
        float getCurrentPercent() { return 100f - currentWaterArea / WATER_AREA * 100; }

        void clearTrack() { // стирает пути ксоника
            for (int y = 0; y < 46; y++)
                for (int x = 0; x < 64; x++)
                    if (field[x][y] == colorTrack) field[x][y] = colorWater;
        }

        void fillTemporary(int x, int y) { // временное заполнение зоны
            if (field[x][y] > colorWater) return;
            field[x][y] = colorTemp; // заполнение временным цветом
            for (int dx = -1; dx < 2; dx++)
                for (int dy = -1; dy < 2; dy++) fillTemporary(x + dx, y + dy);
        }

        void tryToFill() { //рекурсивный метод / проблемы с закраской отрезаных кусоков
            currentWaterArea = 0;
            for (Ball ball : balls.getBalls()) fillTemporary(ball.getX(), ball.getY()); //
            for (int y = 0; y < 46; y++)
                for (int x = 0; x < 64; x++) {
                    if (field[x][y] == colorTrack || field[x][y] == colorWater) {
                        field[x][y] = colorLand;
                        countScore += 10;
                    }
                    if (field[x][y] == colorTemp) {
                        field[x][y] = colorWater;
                        currentWaterArea++;
                    }
                }
        }

        void paint(Graphics g) {
            for (int y = 0; y < 46; y++)
                for (int x = 0; x < 64; x++) {
                    g.setColor(new Color(field[x][y]));
                    g.fillRect(x* 10, y* 10, 10, 10);
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
            x = 64 / 2;
            direction = 0;
            isWater = false;
        }

        int getX() { return x; }
        int getY() { return y; }
        int getCountLives() { return countLives; }

        void decreaseCountLives() { countLives--; }

        void setDirection(int direction) { this.direction = direction; } //меняет направление ксоника

        void move() {
            if (direction == 37) x--;
            if (direction == 39) x++;
            if (direction == 38) y--;
            if (direction == 40) y++;
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if (y > 46 - 1) y = 46 - 1;
            if (x > 64 - 1) x = 64 - 1;
            isSelfCross = field.getColor(x, y) == colorTrack; //налетел ли наслед свой
            if (field.getColor(x, y) == colorLand && isWater) { //если ксоник с воды перешел наземлю
                direction = 0;
                isWater = false;
                field.tryToFill();
            }
            if (field.getColor(x, y) == colorWater) {
                isWater = true;
                field.setColor(x, y, colorTrack);
            }
        }

        boolean isSelfCrosed() { return isSelfCross; }

        void paint(Graphics g) {
            g.setColor((field.getColor(x, y) == colorLand) ? new Color(colorTrack) : Color.white);
            g.fillRect(x* 10, y* 10, 10, 10);
            g.setColor((field.getColor(x, y) == colorLand) ? Color.white : new Color(colorTrack));
            g.fillRect(x* 10 + 3, y* 10 + 3, 10 - 6, 10 - 6);
        }
    }

    class Balls {
        private ArrayList<Ball> balls = new ArrayList<>();

        Balls() {
            add();
        }

        void add() { balls.add(new Ball()); } //добавление шарика

        void move() { for (Ball ball : balls) ball.move(); } //перемещение шариков/ foreach

        ArrayList<Ball> getBalls() { return balls; }

        boolean isHitTrackOrXonix() { //столкновение с ксоником
            for (Ball ball : balls) if (ball.isHitTrackOrXonix()) return true;
            return false;
        }

        void paint(Graphics g) { for (Ball ball : balls) ball.paint(g); }
    }

    class Ball {
        private int x, y, dx, dy; //dx - смещение

        Ball() {
            do { //проверка места объекта в поле
                x = random.nextInt(64);
                y = random.nextInt(46);
            } while (field.getColor(x, y) > colorWater);
            dx = random.nextBoolean()? 1 : -1;
            dy = random.nextBoolean()? 1 : -1;
        }

        void updateDXandDY() {//изменение
            if (field.getColor(x + dx, y) == colorLand) dx = -dx;
            if (field.getColor(x, y + dy) == colorLand) dy = -dy;
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
            return field.getColor(x + dx, y + dy) == colorTrack || x + dx == xonix.getX() && y + dy == xonix.getY();
        }

        void paint(Graphics g) {
            g.setColor(Color.white);
            g.fillOval(x* 10, y* 10, 10, 10);
            g.setColor(new Color(colorLand));
            g.fillOval(x* 10 + 2, y* 10 + 2, 10 - 4, 10 - 4);
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
        private final String GAME_OVER_MSG = "Game Over";
        private boolean gameOver;

        boolean isGameOver() { return gameOver; }

        void paint(Graphics g) {
            if (xonix.getCountLives() == 0) {
                gameOver = true;
                g.setColor(Color.white);
                g.setFont(new Font("", Font.BOLD, 60));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(GAME_OVER_MSG, (64 * 10 + 6 - fm.stringWidth(GAME_OVER_MSG))/2, (46 * 10)/2);
            }
        }
    }

    class Canvas extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            field.paint(g);
            xonix.paint(g);
            balls.paint(g);
            gameover.paint(g);
        }
    }
}