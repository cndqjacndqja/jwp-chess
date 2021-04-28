package chess.repository;

import chess.dao.BoardDao;
import chess.dao.RoomDao;
import chess.domain.board.Board;
import chess.domain.board.Position;
import chess.domain.piece.Piece;
import chess.domain.piece.Team;
import chess.dto.BoardDto;
import chess.dto.MoveInfoDto;
import chess.dto.RoomsDto;
import chess.dto.TurnDto;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {
    private final RoomDao roomDao;
    private final BoardDao boardDao;

    public RoomRepository(RoomDao roomDao, BoardDao boardDao) {
        this.roomDao = roomDao;
        this.boardDao = boardDao;
    }

    public BoardDto createRoom(String roomName) {
        int roomId = roomDao.createRoom(roomName);
        return boardDao.createBoard(roomId);
    }

    public void resetBoard(Board resetBoard, int roomId) {
        roomDao.resetTurnOwner(roomId);
        boardDao.resetBoard(resetBoard, roomId);
    }

    public RoomsDto findAll() {
        return roomDao.findRoomList();
    }

    public BoardDto findBoardByRoomId(int roomId) {
        return boardDao.findBoardByRoomId(roomId);
    }

    public void updateBoard(MoveInfoDto moveInfoDto, Board board, int roomId) {
        String target = moveInfoDto.getTarget();
        String destination = moveInfoDto.getDestination();
        Position targetPosition = Position.from(target);
        Piece targetPiece = board.getBoard().get(targetPosition);
        Team turn = findTurnAfterMove(board, moveInfoDto, roomId);

        boardDao.updateBoardAfterMove(target, destination, targetPiece, roomId);
        roomDao.updateTurnOwnerAfterMove(turn, roomId);
    }

    private Team findTurnAfterMove(Board board, MoveInfoDto moveInfoDto, int roomId) {
        String target = moveInfoDto.getTarget();
        String destination = moveInfoDto.getDestination();
        TurnDto previousTurn = roomDao.findTurnOwnerByRoomId(roomId);
        Team nextTurn = Team.convertStringToTeam(previousTurn.getTurn());
        return board.movePiece(Position.from(target), Position.from(destination), nextTurn);
    }

    public boolean exists(String roomName) {
        return roomDao.isExistName(roomName);
    }
}