import {getFetch, postFetch} from "./promise/fetches.js"

const chessGameId = getParameterByName("id");
const $startButton = document.querySelector("#start");
const $resetButton = document.querySelector("#reset");
const $chessboard = document.querySelector("#chess-board");
const $blackScore = document.querySelector("#BLACK");
const $whiteScore = document.querySelector("#WHITE");
const $turn = document.querySelector("#turn");
const $title = document.querySelector("#title");

$startButton.addEventListener("click", start);
$resetButton.addEventListener("click", reset);
$chessboard.addEventListener("click", onClickPiece);

async function start() {
    await initializeChessBoard();
    $startButton.disabled = 'disabled';
}

async function reset() {
    const piecesData = await getFetch(`/games/${chessGameId}/reset`);
    await updateBoard(piecesData);
}

async function initializeChessBoard() {
    const piecesData = await getFetch(`/games/${chessGameId}/load/`);
    await calculateScore();
    setBoard(piecesData.piecesAndPositions);
    setTurn(piecesData.color);
    setTitle(piecesData.title)
}

async function updateBoard(piecesData) {
    await calculateScore();
    setBoard(piecesData.piecesAndPositions);
    setTurn(piecesData.color);
    setTitle(piecesData.title);
}

function setBoard(positionsAndPieces) {
    $chessboard.querySelectorAll(".box").forEach(e => {
        e.innerHTML = ""
    });
    Object.keys(positionsAndPieces).forEach(e => {
        const coordinate = $chessboard.querySelector('#' + e)
        let piece = document.createElement("img");
        piece.src = `../img/${positionsAndPieces[e].color}-${positionsAndPieces[e].notation}.png`
        piece.className = "piece";
        piece.id = positionsAndPieces[e].notation;
        piece.style.width = "100%";
        piece.style.height = "100%";
        coordinate.appendChild(piece);
    });
}

function setTitle(title) {
    $title.innerHTML = title + "  Room";
}

async function calculateScore() {
    const scoreResponseData = await getFetch(`/games/${chessGameId}/score`);

    $blackScore.innerHTML = `<h2>BLACK SCORE</h2><br><h1>${scoreResponseData.colorsScore.BLACK}</h1>`;
    $whiteScore.innerHTML = `<h2>WHITE SCORE</h2><br><h1>${scoreResponseData.colorsScore.WHITE}</h1>`;
}

async function onClickPiece(e) {
    if (e.target.classList.contains("box")) {
        if (checkAnySelected()) {
            await movePiece(getFirstSelected().id, e.target.id);
            await calculateScore();
            removeSelectedClass();
            return;
        }

        e.target.classList.toggle("selected");
    }
    if(e.target.parentNode.classList.contains("box")) {
        if (checkAnySelected()) {
            await movePiece(getFirstSelected().id, e.target.parentNode.id);
            await calculateScore();
            removeSelectedClass();
            return;
        }

        e.target.parentNode.classList.toggle("selected");
    }
}

function checkAnySelected() {
    return $chessboard.querySelectorAll(".selected").length === 1;
}

function getFirstSelected() {
    return $chessboard.querySelector(".selected");
}

function removeSelectedClass() {
    $chessboard.querySelectorAll(".selected")
        .forEach(e => e.classList.remove("selected"));
}

async function movePiece(from, to) {
    await postFetch(`/games/${chessGameId}/move`, {from: from, to: to})
        .then(function (moveResult){
            if (moveResult.hasOwnProperty("end") && moveResult.end === true) {
                alert("게임이 종료되었습니다~!");
            }
            if (moveResult.hasOwnProperty("end") && moveResult.end === false) {
                initializeChessBoard();
            }
        })
        .catch(removeSelectedClass);
}

function setTurn(turn) {
    if (turn === "WHITE") {
        $turn.innerText = `🏳️ ${turn} TURN 🏳️`;
    }
    if (turn === "BLACK") {
        $turn.innerText = `🏴 ${turn} TURN 🏴`;
    }
}

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}