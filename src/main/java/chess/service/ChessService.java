package chess.service;

import chess.dao.BoardDao;
import chess.dao.RoomDao;
import chess.domain.Score;
import chess.domain.Team;
import chess.domain.piece.Blank;
import chess.domain.piece.Piece;
import chess.domain.piece.Pieces;
import chess.domain.position.Position;
import chess.domain.state.BlackTurn;
import chess.domain.state.BoardInitialize;
import chess.domain.state.GameState;
import chess.domain.state.Playing;
import chess.domain.state.WhiteTurn;
import chess.dto.request.CreateRoomDto;
import chess.dto.response.BoardDto;
import chess.dto.response.GameStateDto;
import chess.dto.response.PieceDto;
import chess.dto.response.RoomDto;
import chess.dto.response.ScoreDto;
import chess.dto.response.StatusDto;
import chess.entity.Room;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChessService {

    private final BoardDao boardDao;
    private final RoomDao roomDao;

    public ChessService(BoardDao boardDao, RoomDao roomDao) {
        this.boardDao = boardDao;
        this.roomDao = roomDao;
    }

    public void createRoom(CreateRoomDto room) {
        Long roomId = roomDao.save(room.getTitle(), room.getPassword());
        boardDao.saveAll(BoardInitialize.create(), roomId);
    }

    public List<RoomDto> findRooms() {
        List<Room> rooms = roomDao.findAll();
        return rooms.stream()
            .map(room -> new RoomDto(room.getId(), room.getTeam(), room.getTitle(),
                room.getStatus()))
            .collect(Collectors.toList());
    }

    public void deleteBy(Long roomId, String password) {
        Room room = getRoom(roomId);
        room.validateCanDelete(password);
        roomDao.deleteBy(roomId, password);
    }

    private Room getRoom(Long roomId) {
        Optional<Room> room = roomDao.findById(roomId);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("Room이 존재하지 않습니다.");
        }
        return room.get();
    }

    public ScoreDto getScoreBy(Long roomId) {
        GameState gameState = getGameState(roomId);
        Score score = new Score(gameState.getBoard());
        return new ScoreDto(score.getTotalScoreWhiteTeam(), score.getTotalScoreBlackTeam());
    }

    private GameState getGameState(Long roomId) {
        Room room = getRoom(roomId);
        Map<Position, Piece> board = getPiecesOfRoom(room);
        return createTurn(room, board);
    }

    private Map<Position, Piece> getPiecesOfRoom(Room room) {
        List<PieceDto> pieces = boardDao.findAll(room.getId());
        Map<Position, Piece> board = new HashMap<>();
        for (PieceDto pieceOfPieces : pieces) {
            Piece piece = Pieces.find(pieceOfPieces.getSymbol());
            Position position = Position.from(pieceOfPieces.getPosition());
            board.put(position, piece);
        }
        return board;
    }

    private Playing createTurn(Room room, Map<Position, Piece> board) {
        Team status = room.getTeam();
        if (status.isWhiteTeam()) {
            return new WhiteTurn(board);
        }
        return new BlackTurn(board);
    }

    public BoardDto getBoard(Long roomId) {
        List<PieceDto> pieces = boardDao.findAll(roomId);
        Room room = getRoom(roomId);
        return new BoardDto(pieces, room.getTeam());
    }

    public BoardDto resetBy(Long roomId) {
        boardDao.delete(roomId);
        boardDao.saveAll(BoardInitialize.create(), roomId);
        roomDao.updateTeam(Team.WHITE, roomId);
        roomDao.updateStatus(roomId, true);
        return getBoard(roomId);
    }

    public GameStateDto findGameStateBy(Long roomId) {
        Room room = getRoom(roomId);
        return new GameStateDto(room.getTeam(), room.getStatus());
    }

    public GameStateDto endBy(Long roomId) {
        roomDao.updateStatus(roomId, false);
        GameState gameState = getGameState(roomId);
        Score score = new Score(gameState.getBoard());
        if (score.getTotalScoreWhiteTeam() > score.getTotalScoreBlackTeam()) {
            return new GameStateDto(Team.WHITE, false);
        }
        return new GameStateDto(Team.BLACK, false);
    }

    public GameStateDto move(Long roomId, String source, String destination) {
        GameState gameState = getGameState(roomId);
        Piece sourcePiece = gameState.getPiece(Position.from(source));
        gameState = gameState.move(source, destination);

        boardDao.updatePosition(Blank.SYMBOL, source, roomId);
        boardDao.updatePosition(sourcePiece.getSymbol(), destination, roomId);

        if (!gameState.isRunning()) {
            roomDao.updateStatus(roomId, false);
            return new GameStateDto(gameState.getTeam(), gameState.isRunning());
        }
        roomDao.updateTeam(gameState.getTeam(), roomId);
        return new GameStateDto(gameState.getTeam(), gameState.isRunning());
    }

    public StatusDto getStatus(Long roomId) {
        Room room = getRoom(roomId);
        return new StatusDto(room.getStatus());
    }
}
