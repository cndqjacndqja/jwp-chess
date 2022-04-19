package chess;

import chess.controller.ChessController;

import static spark.Spark.*;

public class SparkChessApplication {
    public static void main(String[] args) {
        port(8080);
        staticFileLocation("templates");
        ChessController chessController = new ChessController();
        chessController.run();
    }
}
