package org.example;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Main {
    public enum GameStatus{
        GAME_IN_PROGRESS,GAME_OVER,UNKNOWN_STATE
    }

    public enum ScreenState{
        START_MENU,MAIN_MENU_ITEMS_LIST,SET_GRID_MENU_SIZE,USER_SET_GRID_SIZE,SET_DIFFICULTY_LEVEL,START_GAME,EXIT_GAME,BACK_TO_MAIN_MENU,BACK_TO_SELECT_GRID_SIZE,UNKNOWN_STATE
    }

    public enum PlayerDirection{
        UP,DOWN,LEFT,RIGHT,UNKNOWN
    }

    private static Screen screen;
    private static TerminalSize size;
    private static GameStatus gameStatus;
    private static ScreenState screenState;
    private static PlayerDirection playerDirection;

    private static final Random RAND = new Random();
    private static final String[] START_GAME_MENU = {"Start Game","Exit Game"};
    private static final String[] MAIN_MENU_ITEMS = {"Choose Preset Size","Set Custom Size","Back to Main"};
    private static final String[] GRID_GAME_BOARD_SIZE = {"10 x 10", "15 x 15", "20 x 20","30 x 30", "40 x 40", "50 x 50","Back"};
    private static final String[] DIFFICULTY_LEVEL = {"Very Easy","Easy","Medium","Hard","Very Hard","Back"};
    private static ArrayList<int[]>monsterList = new ArrayList<>();
    private static ArrayList<int[]>coinList = new ArrayList<>();
    private static ArrayList<int[]>trapList = new ArrayList<>();
    private static int selectedMainIndex = 0;
    private static int selectedBoardMenuIndex = 0;
    private static int selectedGridIndex = 0;
    private static int selectedDifficultyIndex = 0;
    private static int monsterCount = 0;
    private static int coinCount = 0;
    private static int trapCount;
    private static int x;
    private static int y;
    private static int width;
    private static int height;
    private static int playerScore = 0;
    private static int monsterScore = 0;
    private static int rowStartingPosition = 0;
    private static int colStartingPosition = 0;
    private static boolean isGameRunning = true;

    private static final int MAX_COL_GRID_SIZE = 100;
    private static final int MAX_ROW_GRID_SIZE = 100;

    private static int boardStartRow = 0;
    private static int boardStartCol = 0;
    private static int screenRow = 0;
    private static int screenCol = 0;
    private static String[][] gameGrid = new String[MAX_COL_GRID_SIZE][MAX_ROW_GRID_SIZE];
    private static int gridSize;
    private static int row;
    private static int col;
    private static String winnerMessage = "";

    public static void main(String[] args) {
        try {
            createScreen();
            runGameLoop();
            screen.stopScreen();
        }catch(IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void runGameLoop() throws IOException, InterruptedException {
        gameStatus = GameStatus.GAME_IN_PROGRESS;
        screenState = ScreenState.START_MENU;
        while(isGameRunning) {
            screen.clear();
            renderCurrentScreen();
            if(gameStatus == GameStatus.GAME_OVER) {
                adjustWinnerMessage();
                screen.refresh();
                Thread.sleep(3000);
                screenState = ScreenState.START_MENU;
                selectedMainIndex = 0;
                winnerMessage = "";
                resetGame();
                continue;
            }
            screen.refresh();
            screenRouting();
            Thread.sleep(16);
        }
    }

    private static void resetGame() {
        playerScore = 0;
        monsterScore = 0;
        rowStartingPosition = gridSize / 2;
        colStartingPosition = gridSize / 2;
        monsterList.clear();
        coinList.clear();
        initializeGameBoard();
        gameStatus = GameStatus.GAME_IN_PROGRESS;
        winnerMessage = "";
    }

    private static void createScreen()throws IOException{
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        factory.setInitialTerminalSize(new TerminalSize(100, 100));
        screen = factory.createScreen();
        screen.startScreen();
        size = screen.getTerminalSize();
        width = getWidth();
        height = getHeight();
        x = calculateHeaderX();
        y = calculateHeaderY();
        screen.setCursorPosition(null);
    }

    private static void screenRouting()throws IOException{
        if(screenState == ScreenState.START_MENU) {
            selectedMainIndex = getKeyStroke(selectedMainIndex,START_GAME_MENU);
        }else if(screenState == ScreenState.MAIN_MENU_ITEMS_LIST) {
            selectedBoardMenuIndex = getKeyStroke(selectedBoardMenuIndex,MAIN_MENU_ITEMS);
        }else if(screenState == ScreenState.SET_GRID_MENU_SIZE) {
            selectedGridIndex = getKeyStroke(selectedGridIndex,GRID_GAME_BOARD_SIZE);
        }else if(screenState == ScreenState.SET_DIFFICULTY_LEVEL){
            selectedDifficultyIndex = getKeyStroke(selectedDifficultyIndex,DIFFICULTY_LEVEL);
        }else if(screenState == ScreenState.START_GAME) {
            setInputDirection();
            determineWinner();
        }
    }

    private static int getKeyStroke(int index,String[]arrItems)throws IOException{
        KeyStroke key = screen.pollInput();
        if(key!=null) {
            switch(key.getKeyType()) {
                case ArrowUp ->{
                    index = (index - 1 + arrItems.length)%arrItems.length;
                }
                case ArrowDown ->{
                    index = (index + 1)%arrItems.length;
                }
                case Enter ->{
                    handleEnterButton();
                }
                case Escape ->{
                    isGameRunning = false;
                }
            }
        }
        return index;
    }

    private static void handleEnterButton() {
        switch(screenState) {
            case START_MENU:
                checkForExit();
                handleEnterAction(selectedMainIndex,0,ScreenState.MAIN_MENU_ITEMS_LIST);
                break;

            case MAIN_MENU_ITEMS_LIST:
                handleEnterAction(selectedBoardMenuIndex,0,ScreenState.SET_GRID_MENU_SIZE);
                handleEnterAction(selectedBoardMenuIndex,1,ScreenState.USER_SET_GRID_SIZE);
                handleEnterAction(selectedBoardMenuIndex,2,ScreenState.START_MENU);
                break;

            case SET_GRID_MENU_SIZE:
                checkIsBackSelected();
                break;

            case SET_DIFFICULTY_LEVEL:
                determineGameDifficulty();
                break;
        }
    }

    private static void checkForExit(){
        if(selectedMainIndex == 1){
            isGameRunning = false;
        }
    }

   private static void determineGameDifficulty(){
        if(selectedDifficultyIndex == 5){
            screenState = ScreenState.SET_GRID_MENU_SIZE;
        }else{
            applyDifficultySettings();
            monsterList.clear();
            coinList.clear();
            screenState = ScreenState.START_GAME;
            setupNewGame();
        }
    }

    private static void checkIsBackSelected() {
        if(selectedGridIndex == 6) {
            screenState = ScreenState.MAIN_MENU_ITEMS_LIST;
        }else {
            updateGridBoardSize();
            setupNewGame();
        }
    }

    private static void handleEnterAction(int index,int items,ScreenState targetScreenState) {
        if(index == items) {
            screenState = targetScreenState;
        }
    }

    private static void renderCurrentScreen() throws IOException {
        switch(screenState) {
            case START_MENU:
                drawOperationHeading(Headers.GAME_HEADER_TITLE);
                drawMenu(selectedMainIndex,START_GAME_MENU,Headers.GAME_HEADER_TITLE);
                break;

            case MAIN_MENU_ITEMS_LIST:
                drawOperationHeading(Headers.MAIN_MENU_HEADER1);
                drawMenu(selectedBoardMenuIndex,MAIN_MENU_ITEMS,Headers.MAIN_MENU_HEADER1);
                break;

            case SET_GRID_MENU_SIZE:
                drawOperationHeading(Headers.SET_GRID_SIZE_HEADER);
                drawMenu(selectedGridIndex,GRID_GAME_BOARD_SIZE,Headers.SET_GRID_SIZE_HEADER);
                break;

            case SET_DIFFICULTY_LEVEL:
                drawOperationHeading(Headers.PICK_DIFFICULTY_HEADER);
                drawMenu(selectedDifficultyIndex,DIFFICULTY_LEVEL,Headers.PICK_DIFFICULTY_HEADER);
                break;

            case START_GAME:
                drawOperationHeading(Headers.START_GAME_HEADER);
                drawGameBoard();
                adjustScorePosition();
                break;
        }
    }

    private static void adjustScorePosition() throws IOException {
        if(gridSize == 50){
            displayScore(5,40,"Player Score:",playerScore);
            displayScore(5,41,"Monster Score:",monsterScore);
        }else{
            displayScore(10,40,"Player Score: ",playerScore);
            displayScore(10,41,"Monster Score: ",monsterScore);
        }
    }

    private static void adjustWinnerMessage() {
        if (gridSize == 50) {
            int row = y + -5;
            int col = (width - winnerMessage.length()) / 2;
            displayWhoWon(row, col, winnerMessage);
        } else if (gridSize == 40) {
            int row = y + 1;
            int col = (width - winnerMessage.length()) / 2;
            displayWhoWon(row, col, winnerMessage);
        } else {
            int row = boardStartRow - 2;
            int col = boardStartCol + (gridSize / 2) - (winnerMessage.length() / 2);
            displayWhoWon(row, col, winnerMessage);
        }
    }

    private static void displayScore(int row,int col,String playerName,int score) throws IOException {
        TextGraphics tg = screen.newTextGraphics();
        String textToDraw = playerName + " " + score;
        tg.putString(row,col,textToDraw);
    }

    private static void determineWinner(){
        if(coinList.size() == 0){
            gameStatus = GameStatus.GAME_OVER;
            if(playerScore > monsterScore){
                winnerMessage = "PLAYER WON!";
            }else if(monsterScore > playerScore){
                winnerMessage = "MONSTERS WIN!";
            }else{
                winnerMessage = "DRAW!";
            }
        }
    }

    private static void displayWhoWon(int row,int col,String player){
        TextGraphics tg = screen.newTextGraphics();
        tg.putString(row,col,player);
    }

    private static boolean isHandleCoinCollection(){
        if (!checkBounds(row, col) && "C".equals(gameGrid[row][col])) {
            for(int i = 0; i < coinList.size(); i++){
                int[] coinPosition = coinList.get(i);
                if(coinPosition[0] == row && coinPosition[1] == col){
                    coinList.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    private static void drawMenu(int index,String[]menuItems,String[]headerArt)throws IOException{
        TextGraphics tg = screen.newTextGraphics();
        int startY = y + headerArt.length + 2;
        for(int i = 0; i <menuItems.length; i++) {
            String item = menuItems[i];
            int menuX = (width - item.length())/2;
            int menuY = startY + i;
            if(i == index) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.WHITE);
                tg.putString(menuX, menuY, item);
                tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            }else {
                tg.putString(menuX,menuY,item);
            }
        }
    }

    private static void drawOperationHeading(String[]header) {
        TextGraphics g = screen.newTextGraphics();
        for(int i = 0; i < header.length; i++) {
            g.putString(x, y + i,header[i]);
        }
    }

    private static int getWidth() {
        return size.getColumns();
    }

    private static  int getHeight() {
        return size.getRows();
    }

    private static int calculateHeaderX() {
        int x = (width - Headers.GAME_HEADER_TITLE[0].length())/2;
        return Math.max(0, x);
    }

    private static int calculateHeaderY() {
        double topFraction = 0.16;
        int y = (int)(height * topFraction);
        return Math.max(0, y);
    }

    private static void setInputDirection()throws IOException{
        KeyStroke k = screen.pollInput();
        if(k == null || k.getKeyType() != KeyType.Character){
            return;
        }
        char c = Character.toLowerCase(k.getCharacter());
        switch(c) {
            case 'w':
                playerDirection = PlayerDirection.UP;
                movePlayer(-1,0);
                break;

            case 'a':
                playerDirection = PlayerDirection.LEFT;
                movePlayer(0,-1);
                break;

            case 's':
                playerDirection = PlayerDirection.DOWN;
                movePlayer(1,0);
                break;

            case 'd':
                playerDirection = PlayerDirection.RIGHT;
                movePlayer(0,1);
                break;

            default:
                return;
        }
        if(checkBounds(rowStartingPosition,colStartingPosition)) {
            rowStartingPosition = RAND.nextInt(gridSize);
            colStartingPosition = RAND.nextInt(gridSize);
        }
        monsterMovement();
    }

    private static void movePlayer(int deltaRow,int deltaCol){
        int oldRow = rowStartingPosition;
        int oldCol = colStartingPosition;
        rowStartingPosition +=deltaRow;
        colStartingPosition +=deltaCol;
        syncPlayerPosition();
        if(hasPlayerOrMonsterPickedUpCoin("P")){
            playerScore++;
        }
        gameGrid[oldRow][oldCol] = "*";
    }

    private static void monsterMovement(){
        for(int i = 0; i < monsterList.size(); i++){
            int monsterPosition[] = monsterList.get(i);
            int oldRow = monsterPosition[0];
            int oldCol = monsterPosition[1];
            row = oldRow;
            col = oldCol;
            moveMonsterBasedOnDirection();
            handleOutOfBounds();
            monsterPosition[0] = row;
            monsterPosition[1] = col;
            gameGrid[oldRow][oldCol] = "*";
            gameGrid[row][col] = "M";
        }
    }

    private static void handleOutOfBounds(){
        if(checkBounds(row,col)){
            row = RAND.nextInt(gridSize);
            col = RAND.nextInt(gridSize);
        }
    }

    private static void moveMonsterBasedOnDirection(){
        switch(playerDirection){
                case UP:
                    moveMonsterLeft();
                    break;

                case DOWN:
                    moveMonsterRight();
                    break;

                case LEFT:
                    moveMonsterDown();
                    break;

                case RIGHT:
                    moveMonsterForward();
                    break;

                default:
                    System.out.println("ERROR: SOMETHING WENT WRONG!!!");
            }
    }

    private static boolean checkBounds(int row,int col) {
        return isMovementOutOfBounds(row,col);
    }

    private static void updateGridBoardSize() {
        switch(selectedGridIndex) {
            case 0: gridSize = 10;
                setProperties();
                break;

            case 1: gridSize = 15;
                setProperties();
                break;

            case 2: gridSize = 20;
                setProperties();
                break;

            case 3: gridSize = 30;
                setProperties();
                break;

            case 4: gridSize = 40;
                setProperties();
                break;

            case 5: gridSize = 50;
                setProperties();
                break;

            case 6: screenState = ScreenState.BACK_TO_MAIN_MENU;
                break;

            default: System.out.println("SOMETHING WENT WRONG!!!!");
        }
    }

    private static void applyDifficultySettings(){
        switch(selectedDifficultyIndex){
            case 0: monsterCount = 1;
                    coinCount = 5;
                    trapCount = 1;
                break;

            case 1: monsterCount = 2;
                    coinCount = 4;
                    trapCount = 2;
                break;

            case 2: monsterCount = 3;
                    coinCount = 3;
                    trapCount = 3;
                break;

            case 3: monsterCount = 4;
                    coinCount = 2;
                    trapCount = 4;
                break;

            case 4: monsterCount = 5;
                    coinCount = 1;
                    trapCount = 5;
                break;

            case 5: screenState = ScreenState.BACK_TO_SELECT_GRID_SIZE;
                break;

            default: System.out.println("ERROR: SOMETHING WENT WRONG WITH NUMBER OF MONSTERS!!!");
        }
    }

    private static void setProperties() {
        boardStartRow = y + Headers.START_GAME_HEADER.length;
        boardStartCol = (width - gridSize)/2;
        screenState = ScreenState.SET_DIFFICULTY_LEVEL;
    }

    private static void initializeGameBoard() {
        for(int i = 0; i < gridSize; i++) {
            for(int j = 0; j < gridSize; j++) {
                gameGrid[i][j] = "*";
            }
        }
    }

    private static void setupNewGame() {
        initializeGameBoard();
        rowStartingPosition = gridSize / 2;
        colStartingPosition = gridSize /2;
        placeEntities(monsterList,monsterCount,"M");
        placeEntities(coinList,coinCount,"C");
        placeEntities(trapList,trapCount,"T");
    }

    private static void drawGameBoard() {
        TextGraphics tg = screen.newTextGraphics();
        gameGrid[rowStartingPosition][colStartingPosition] = "P";
        tg.setForegroundColor(TextColor.ANSI.BLUE_BRIGHT);
        for(int i = 0; i < gridSize; i++) {
            for(int j = 0; j < gridSize; j++) {
                String cell = gameGrid[i][j];
                screenRow = boardStartRow + i;
                screenCol  = boardStartCol + j;
                tg.putString(screenCol, screenRow,cell);
            }
        }
        entityColor(coinList, TextColor.ANSI.YELLOW_BRIGHT,"C");
        entityColor(monsterList,TextColor.ANSI.RED_BRIGHT,"M");
        entityColor(trapList,TextColor.ANSI.MAGENTA_BRIGHT,"T");
    }

    private static void entityColor(ArrayList<int[]>list,TextColor textColor,String entity){
        TextGraphics tg = screen.newTextGraphics();
        for(int i = 0; i < list.size(); i++){
            row = list.get(i)[0];
            col = list.get(i)[1];
            screenRow = boardStartRow + row;
            screenCol = boardStartCol + col;
            if(gameGrid[row][col].equals(entity)){
                tg.setForegroundColor(textColor);
                tg.putString(screenCol,screenRow,entity);
            }
        }
    }

    private static void syncPlayerPosition(){
        row = rowStartingPosition;
        col = colStartingPosition;
    }

    private static void moveMonsterForward() {
        row--;
        if(hasPlayerOrMonsterPickedUpCoin("M")){
            monsterScore++;
        }
    }

    private static void moveMonsterLeft() {
        col--;
        if(hasPlayerOrMonsterPickedUpCoin("M")){
            monsterScore++;
        }
    }

    private static void moveMonsterRight() {
        col++;
        if(hasPlayerOrMonsterPickedUpCoin("M")){
            monsterScore++;
        }
    }

    private static void moveMonsterDown() {
        row++;
        if(hasPlayerOrMonsterPickedUpCoin("M")){
            monsterScore++;
        }
    }

    private static boolean hasPlayerOrMonsterPickedUpCoin(String player){
        if(isHandleCoinCollection()){
            gameGrid[row][col] = player;
            return true;
        }
        return false;
    }

    private static boolean isMovementOutOfBounds(int row,int col) {
        if((row < 0 || row >= gridSize) || (col < 0 || col >= gridSize)){
            return true;
        }
        return false;
    }

    private static void placeEntities(ArrayList<int[]>list,int entityCount,String value){
        while(list.size() < entityCount){
            row = RAND.nextInt(gridSize);
            col = RAND.nextInt(gridSize);
            if(!(row == rowStartingPosition && col == colStartingPosition) && (gameGrid[row][col] == "*")){
                gameGrid[row][col] = value;
                int pos[] = new int[2];
                pos[0] = row;
                pos[1] = col;
                list.add(pos);
            }
        }
    }





























































































    /*private static boolean isHandleCoinCollection(){
        if(gameGrid[row][col].equals("C")){
            for(int i = 0; i < coinList.size(); i++){
                int coinPosition[] = coinList.get(i);
                int coinRow = coinPosition[0];
                int coinCol = coinPosition[1];
                if(coinRow == row && coinCol == col){
                    coinList.remove(coinPosition);
                    return true;
                }
            }
        }
        return false;
    }*/








    /*private static void monsterMovement() {
        //int oldRow = monsterStartingPositionRow;
        //int oldCol = monsterStartingPositionCol;
        switch(playerDirection) {
            case UP:
                moveMonsterLeft();
                break;

            case DOWN:
                moveMonsterRight();
                break;

            case LEFT:
                moveMonsterDown();
                break;

            case RIGHT:
                moveMonsterForward();
                break;

            case UNKNOWN:
                System.out.println("ERROR: PROBLEM WITH MOVEMENT! ");
                break;
        }

        for(int i = 0; i < monsterList.size(); i++){
            int monsterPosition[] = monsterList.get(i);
            int oldRow = monsterPosition[0];
            int oldCol = monsterPosition[1];
            row = oldRow;
            col = oldCol;

            if(checkBounds(row,col)){
                row = RAND.nextInt(gridSize);
                col = RAND.nextInt(gridSize);
                monsterPosition[0] = row;
                monsterPosition[1] = col;

            }
            gameGrid[oldRow][oldCol] = "*";
            gameGrid[row][col] = "M";
        }
        if(checkBounds(monsterStartingPositionRow,monsterStartingPositionCol)) {
            monsterStartingPositionRow = RAND.nextInt(gridSize);
            monsterStartingPositionCol = RAND.nextInt(gridSize);
        }
        gameGrid[oldRow][oldCol] = "*";
        gameGrid[row][col] = "M";
    }*/

    /*
    private static void keepTrackOfMonsterPosition(){
        while(monsterList.size() < monsterCount){
            row = RAND.nextInt(gridSize);
            col = RAND.nextInt(gridSize);
            if(row != rowStartingPosition && col != colStartingPosition){
                gameGrid[row][col] = "M";
                int monsterPos[] = new int[2];
                monsterPos[0] = row;
                monsterPos[1] = col;
                monsterList.add(monsterPos);
            }
        }
    }

    private static void keepTrackOfCoinPositions(){
        while(coinList.size() < coinCount){
            row = RAND.nextInt(gridSize);
            col = RAND.nextInt(gridSize);
            if(row != rowStartingPosition && col != colStartingPosition){
                gameGrid[row][col] = "C";
                int coinPos[] = new int[2];
                coinPos[0] = row;
                coinPos[1] = col;
                coinList.add(coinPos);
            }
        }
    }






    private static void keepTrackOfMonsterPosition() {
        boolean isTaken = true;
        while(isTaken) {
            monsterStartingPositionRow = RAND.nextInt(gridSize);
            monsterStartingPositionCol = RAND.nextInt(gridSize);
            if(monsterStartingPositionRow != rowStartingPosition || monsterStartingPositionCol != colStartingPosition) {
                gameGrid[monsterStartingPositionRow][monsterStartingPositionCol] = "M";
                break;
            }
        }
    }*/

    /*private static void keepTrackOfMonsterPosition(){
        while(monsterList.size() < monsterCount){
            monsterStartingPositionRow = RAND.nextInt(gridSize);
            monsterStartingPositionCol = RAND.nextInt(gridSize);
            if (monsterStartingPositionRow != rowStartingPosition || monsterStartingPositionCol != colStartingPosition) {
                gameGrid[monsterStartingPositionRow][monsterStartingPositionCol] = "M";

            }
        }

        private static void monsterMovement(){
    for(int i = 0; i < monsterList.size(); i++){
        int[] monsterPosition = monsterList.get(i);
        int mRow = monsterPosition[0];
        int mCol = monsterPosition[1];

        // move based on direction
        switch(playerDirection){
            case UP -> mCol--;       // moveMonsterLeft
            case DOWN -> mCol++;     // moveMonsterRight
            case LEFT -> mRow++;     // moveMonsterDown
            case RIGHT -> mRow--;    // moveMonsterForward
        }

        // check out of bounds
        if(checkBounds(mRow, mCol)){
            mRow = RAND.nextInt(gridSize);
            mCol = RAND.nextInt(gridSize);
        }

        // collect coin
        if(gameGrid[mRow][mCol] != null && gameGrid[mRow][mCol].equals("C")){
            monsterScore++;
            coinList.removeIf(c -> c[0] == mRow && c[1] == mCol);
        }

        // update grid
        gameGrid[monsterPosition[0]][monsterPosition[1]] = "*";
        gameGrid[mRow][mCol] = "M";

        // update monster list
        monsterPosition[0] = mRow;
        monsterPosition[1] = mCol;
    }
}





    }*/


    /*private static void movePlayerForward() {
        rowStartingPosition--;
        syncPlayerPosition();
        if(checkIfPlayerOrMonsterPickedUpCoin("P")){
            playerScore++;
        }
    }

    private static void movePlayerLeft() {
        colStartingPosition--;
        syncPlayerPosition();
        if(checkIfPlayerOrMonsterPickedUpCoin("P")){
            playerScore++;
        }
    }

    private static void movePlayerRight() {
        colStartingPosition++;
        syncPlayerPosition();
        if(checkIfPlayerOrMonsterPickedUpCoin("P")){
            playerScore++;
        }
    }

    private static void movePlayerDown() {
        rowStartingPosition++;
        syncPlayerPosition();
        if(checkIfPlayerOrMonsterPickedUpCoin("P")){
            playerScore++;
        }
    }*/







}