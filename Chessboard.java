package com.kmfahey.jchessgame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.awt.Image;

public class Chessboard {

    public static final int WHITE = BoardArrays.WHITE;
    public static final int BLACK = BoardArrays.BLACK;

    public static final int PAWN = BoardArrays.PAWN;
    public static final int ROOK = BoardArrays.ROOK;
    public static final int KNIGHT = BoardArrays.KNIGHT;
    public static final int BISHOP = BoardArrays.BISHOP;
    public static final int QUEEN = BoardArrays.QUEEN;
    public static final int KING = BoardArrays.KING;

    public static final int LEFT = BoardArrays.LEFT;
    public static final int RIGHT = BoardArrays.RIGHT;

    public static final HashSet<Integer> VALID_PIECE_INTS = new HashSet<Integer>() {{ 
        this.add(BLACK | KING); this.add(BLACK | QUEEN); this.add(BLACK | ROOK); this.add(BLACK | BISHOP);
        this.add(BLACK | KNIGHT | RIGHT); this.add(BLACK | KNIGHT | LEFT); this.add(BLACK | PAWN);
        this.add(WHITE | KING); this.add(WHITE | QUEEN); this.add(WHITE | ROOK); this.add(WHITE | BISHOP);
        this.add(WHITE | KNIGHT | RIGHT); this.add(WHITE | KNIGHT | LEFT); this.add(WHITE | PAWN);
    }};

    private static final int DOUBLED = 0;
    private static final int ISOLATED = 1;
    private static final int BLOCKED = 2;

    private static final int NORTH = 0;
    private static final int NORTH_NORTH_EAST = 1;
    private static final int NORTH_EAST = 2;
    private static final int EAST_NORTH_EAST = 3;
    private static final int EAST = 4;
    private static final int EAST_SOUTH_EAST = 5;
    private static final int SOUTH_EAST = 6;
    private static final int SOUTH_SOUTH_EAST = 7;
    private static final int SOUTH = 8;
    private static final int SOUTH_SOUTH_WEST = 9;
    private static final int SOUTH_WEST = 10;
    private static final int WEST_SOUTH_WEST = 11;
    private static final int WEST = 12;
    private static final int WEST_NORTH_WEST = 13;
    private static final int NORTH_WEST = 14;
    private static final int NORTH_NORTH_WEST = 15;

    private static final String ALG_NOTN_ALPHA_CHARS = "abcdefgh";
    private static final String ALG_NOTN_NUM_CHARS = "87654321";

    private static final HashMap<Integer, Integer[][]> piecesStartingLocsWhiteBelow = new HashMap<>() {{
        put(BLACK | KING,           new Integer[][] {new Integer[] {3, 0}});
        put(BLACK | QUEEN,          new Integer[][] {new Integer[] {4, 0}});
        put(BLACK | ROOK,           new Integer[][] {new Integer[] {0, 0}, new Integer[] {7, 0}});
        put(BLACK | BISHOP,         new Integer[][] {new Integer[] {2, 0}, new Integer[] {5, 0}});
        put(BLACK | KNIGHT | RIGHT, new Integer[][] {new Integer[] {6, 0}});
        put(BLACK | KNIGHT | LEFT,  new Integer[][] {new Integer[] {1, 0}});
        put(BLACK | PAWN,           new Integer[][] {new Integer[] {0, 1}, new Integer[] {1, 1},
                                                     new Integer[] {2, 1}, new Integer[] {3, 1},
                                                     new Integer[] {4, 1}, new Integer[] {5, 1},
                                                     new Integer[] {6, 1}, new Integer[] {7, 1}});
        put(WHITE | KING,           new Integer[][] {new Integer[] {3, 7}});
        put(WHITE | QUEEN,          new Integer[][] {new Integer[] {4, 7}});
        put(WHITE | ROOK,           new Integer[][] {new Integer[] {0, 7}, new Integer[] {7, 7}});
        put(WHITE | BISHOP,         new Integer[][] {new Integer[] {2, 7}, new Integer[] {5, 7}});
        put(WHITE | KNIGHT | RIGHT, new Integer[][] {new Integer[] {6, 7}});
        put(WHITE | KNIGHT | LEFT,  new Integer[][] {new Integer[] {1, 7}});
        put(WHITE | PAWN,           new Integer[][] {new Integer[] {0, 6}, new Integer[] {1, 6},
                                                     new Integer[] {2, 6}, new Integer[] {3, 6},
                                                     new Integer[] {4, 6}, new Integer[] {5, 6},
                                                     new Integer[] {6, 6}, new Integer[] {7, 6}});
    }};

    private static final HashMap<Integer, Integer[][]> piecesStartingLocsBlackBelow = new HashMap<>() {{
        put(WHITE | KING,           new Integer[][] {new Integer[] {3, 0}});
        put(WHITE | QUEEN,          new Integer[][] {new Integer[] {4, 0}});
        put(WHITE | ROOK,           new Integer[][] {new Integer[] {0, 0}, new Integer[] {7, 0}});
        put(WHITE | BISHOP,         new Integer[][] {new Integer[] {2, 0}, new Integer[] {5, 0}});
        put(WHITE | KNIGHT | RIGHT, new Integer[][] {new Integer[] {6, 0}});
        put(WHITE | KNIGHT | LEFT,  new Integer[][] {new Integer[] {1, 0}});
        put(WHITE | PAWN,           new Integer[][] {new Integer[] {0, 1}, new Integer[] {1, 1},
                                                     new Integer[] {2, 1}, new Integer[] {3, 1},
                                                     new Integer[] {4, 1}, new Integer[] {5, 1},
                                                     new Integer[] {6, 1}, new Integer[] {7, 1}});
        put(BLACK | KING,           new Integer[][] {new Integer[] {3, 7}});
        put(BLACK | QUEEN,          new Integer[][] {new Integer[] {4, 7}});
        put(BLACK | ROOK,           new Integer[][] {new Integer[] {0, 7}, new Integer[] {7, 7}});
        put(BLACK | BISHOP,         new Integer[][] {new Integer[] {2, 7}, new Integer[] {5, 7}});
        put(BLACK | KNIGHT | RIGHT, new Integer[][] {new Integer[] {6, 7}});
        put(BLACK | KNIGHT | LEFT,  new Integer[][] {new Integer[] {1, 7}});
        put(BLACK | PAWN,           new Integer[][] {new Integer[] {0, 6}, new Integer[] {1, 6},
                                                     new Integer[] {2, 6}, new Integer[] {3, 6},
                                                     new Integer[] {4, 6}, new Integer[] {5, 6},
                                                     new Integer[] {6, 6}, new Integer[] {7, 6}});
    }};

    public static final HashMap<String, Integer> pieceStrsToInts = new HashMap<>() {{
        put("white-king",           WHITE | KING);
        put("white-queen",          WHITE | QUEEN);
        put("white-rook",           WHITE | ROOK);
        put("white-bishop",         WHITE | BISHOP);
        put("white-knight-right",   WHITE | KNIGHT | RIGHT);
        put("white-knight-left",    WHITE | KNIGHT | LEFT);
        put("white-pawn",           WHITE | PAWN);
        put("black-king",           BLACK | KING);
        put("black-queen",          BLACK | QUEEN);
        put("black-rook",           BLACK | ROOK);
        put("black-bishop",         BLACK | BISHOP);
        put("black-knight-right",   BLACK | KNIGHT | RIGHT);
        put("black-knight-left",    BLACK | KNIGHT | LEFT);
        put("black-pawn",           BLACK | PAWN);
    }};

    private HashMap<Integer, Image> pieceImages;

    private int colorOnTop;
    private int colorPlaying;
    private static HashMap<String, Piece> piecesLocations;
    private int[][] boardArray;
    private boolean blackKingsRookHasMoved = false;
    private boolean blackQueensRookHasMoved = false;
    private boolean whiteKingsRookHasMoved = false;
    private boolean whiteQueensRookHasMoved = false;
    private boolean blackKingHasMoved = false;
    private boolean whiteKingHasMoved = false;

    public record Piece(int pieceInt, Image pieceImage, int xCoord, int yCoord) { };

    public record Move(Chessboard.Piece movingPiece, int fromXCoord, int fromYCoord,
                int toXCoord, int toYCoord, int capturedPiece, boolean isCastlingKingside,
                boolean isCastlingQueenside) { };

    public Chessboard(final ImagesManager imagesManager, final int playingColor, final int onTopColor) {
        this(null, imagesManager, playingColor, onTopColor);
    }

    public Chessboard(final int[][] boardArrayVal, final ImagesManager imagesManager, final int playingColor,
                      final int onTopColor) {
        colorOnTop = onTopColor;
        colorPlaying = playingColor;

        if (Objects.isNull(boardArrayVal)) {
            boardArray = new int[8][8];
        } else {
            boardArray = boardArrayVal;
        }

        colorOnTop = colorPlaying == WHITE ? BLACK : WHITE;

        HashMap<Integer, Integer[][]> piecesStartingCoords = (colorPlaying == WHITE)
                                                             ? piecesStartingLocsWhiteBelow
                                                             : piecesStartingLocsBlackBelow;

        for (Entry<Integer, Integer[][]> pieceToCoords : piecesStartingCoords.entrySet()) {
            int pieceInt = pieceToCoords.getKey();
            for (Integer[] coords : pieceToCoords.getValue()) {
                int xIdx = coords[0];
                int yIdx = coords[1];
                boardArray[xIdx][yIdx] = pieceInt;
            }
        }

        /* We only need the ImagesManager object to instantiate Piece objects
           with the correct Image 2nd argument. It's not saved to an instance
           variable since it's never used again after this constructor. */

        pieceImages = new HashMap<Integer, Image>();

        for (int pieceInt : VALID_PIECE_INTS) {
            pieceImages.put(pieceInt, imagesManager.getImageByPieceInt(pieceInt));
        }
    }

    public int[] findKing(int kingColor) {
        int xIdx = -1;
        int yIdx = -1;
        for (xIdx = 0; xIdx < 8; xIdx++) {
            for (yIdx = 0; yIdx < 8; yIdx++) {
                if (boardArray[xIdx][yIdx] == (kingColor | KING)) {
                    break;
                }
            }
        }

        return new int[] {xIdx, yIdx};
    }

    public int[][] getBoardArray() {
        return boardArray;
    }

    public int getColorOnTop() {
        return colorOnTop;
    }

    public int getColorPlaying() {
        return colorPlaying;
    }

    public Piece getPieceAtCoords(final int[] coords) {
        return getPieceAtCoords(coords[0], coords[1]);
    }

    public Piece getPieceAtCoords(final int xCoord, final int yCoord) {
        if (boardArray[xCoord][yCoord] == 0) {
            return null;
        }
        int pieceInt = boardArray[xCoord][yCoord];
        return new Piece(pieceInt, pieceImages.get(pieceInt), xCoord, yCoord);
    }

    public int[][] getValidMoveCoordsArray(final int[] coords) throws AlgorithmBadArgumentException {
        return getValidMoveCoordsArray(coords[0], coords[1]);
    }

    public int[][] getValidMoveCoordsArray(final int xCoord, final int yCoord) throws AlgorithmBadArgumentException {
        int usedArrayLength = 0;
        int coordsIndex = 0;
        int[][] movesArray = new int[32][6];
        int[][] movesCoords;
        usedArrayLength = BoardArrays.generatePieceMoves(boardArray, movesArray, 0, xCoord, yCoord,
                                                         colorPlaying, colorOnTop);
        movesCoords = new int[usedArrayLength][2];
        for (int moveIdx = 0; moveIdx < usedArrayLength; moveIdx++) {
            int moveXIdx = movesArray[moveIdx][3];
            int moveYIdx = movesArray[moveIdx][4];
            movesCoords[coordsIndex][0] = moveXIdx;
            movesCoords[coordsIndex][1] = moveYIdx;
            coordsIndex++;
        }
        return movesCoords;
    }

    public boolean isSquareThreatened(final int[] coords, final int opposingColor) {
        return isSquareThreatened(coords[0], coords[1], opposingColor);
    }

    public boolean isSquareThreatened(final int xCoord, final int yCoord, final int opposingColor) {
        int otherColor = opposingColor;
        int thisColor = otherColor == WHITE ? BLACK : WHITE;
        boolean retval;
        /* This operation momentarily changes the boardArray to an invalid state
           to use BoardArray.isKingInCheck() method to detect a threatened
           square. Although this is not a threaded package, synchronized(){} is
           used just in case it's possible that another part of the calling code
           might try to use the boardArray at the same time. */
        synchronized (boardArray) {
            int savedPieceInt = boardArray[xCoord][yCoord];
            boardArray[xCoord][yCoord] = thisColor | KING;
            retval = BoardArrays.isKingInCheck(boardArray, otherColor, colorOnTop);
            boardArray[xCoord][yCoord] = savedPieceInt;
        }
        return retval;
    }

    public boolean isCastlingPossible(int colorOfKing, int kingOrQueen) throws IllegalArgumentException {
        switch (colorOfKing | kingOrQueen) {
            case BLACK | KING -> {
                int yIdx = colorOnTop == BLACK ? 0 : 7;
                if (blackKingHasMoved || blackKingsRookHasMoved) {
                    return false;
                } else if (boardArray[1][yIdx] != 0 || boardArray[2][yIdx] != 0) {
                    return false;
                }
                return true;
            }
            case BLACK | QUEEN -> {
                int yIdx = colorOnTop == BLACK ? 0 : 7;
                if (blackKingHasMoved || blackQueensRookHasMoved) {
                    return false;
                } else if (boardArray[4][yIdx] != 0 || boardArray[5][yIdx] != 0 || boardArray[6][yIdx] != 0) {
                    return false;
                }
                return true;
            }
            case WHITE | KING -> {
                int yIdx = colorOnTop == WHITE ? 0 : 7;
                if (whiteKingHasMoved || whiteKingsRookHasMoved) {
                    return false;
                } else if (boardArray[1][yIdx] != 0 || boardArray[2][yIdx] != 0) {
                    return false;
                }
                return true;
            }
            case WHITE | QUEEN -> {
                int yIdx = colorOnTop == WHITE ? 0 : 7;
                if (whiteKingHasMoved || whiteQueensRookHasMoved) {
                    return false;
                } else if (boardArray[4][yIdx] != 0 || boardArray[5][yIdx] != 0 || boardArray[6][7] != 0) {
                    return false;
                }
                return true;
            }
            default -> {
                throw new IllegalArgumentException("could not resolve arguments to isCastlingPossible()");
            }
        }
    }

    private void movePieceCastling(final Chessboard.Move moveObj) throws KingIsInCheckException, IllegalArgumentException, CastlingNotPossibleException {
        int kingXCoord = moveObj.fromXCoord();
        int kingYCoord = moveObj.fromYCoord();
        int rookXCoord = moveObj.toXCoord();
        int rookYCoord = moveObj.fromYCoord();
        int pieceInt = moveObj.movingPiece().pieceInt();
        boolean isCastlingKingside = moveObj.isCastlingKingside();
        boolean isCastlingQueenside = moveObj.isCastlingQueenside();
        int colorOfPiece = (pieceInt & WHITE) != 0 ? WHITE : BLACK;
        int otherColor = (colorOfPiece == WHITE) ? BLACK : WHITE;
        int rookPieceInt = boardArray[rookXCoord][rookYCoord];
        int savedPieceIntNo1 = 0;
        int savedPieceIntNo2 = 0;
        String colorOfPieceStr = (colorOfPiece == WHITE ? "White" : "Black");

        if ((pieceInt & KING) == 0 || rookPieceInt != (colorOfPiece | ROOK)) {
            throw new IllegalArgumentException("Invalid castling movePiece() parameters.");
        } else if (isCastlingKingside) {
            if (!isCastlingPossible(colorOfPiece, KING)) {
                throw new CastlingNotPossibleException("Castling kingside is not possible for " + colorOfPieceStr);
            }

            savedPieceIntNo1 = boardArray[2][rookYCoord];
            boardArray[2][rookYCoord] = boardArray[rookXCoord][rookYCoord];
            boardArray[rookXCoord][rookYCoord] = 0;

            savedPieceIntNo2 = boardArray[1][kingYCoord];
            boardArray[1][kingYCoord] = boardArray[kingXCoord][kingYCoord];
            boardArray[kingXCoord][kingYCoord] = 0;

            if (BoardArrays.isKingInCheck(boardArray, otherColor, colorOnTop)) {
                boardArray[kingXCoord][kingYCoord] = boardArray[2][kingYCoord];
                boardArray[2][kingYCoord] = savedPieceIntNo2;

                boardArray[rookXCoord][rookYCoord] = boardArray[3][rookYCoord];
                boardArray[3][rookYCoord] = savedPieceIntNo1;

                throw new KingIsInCheckException("Castling kingside would place " + colorOfPieceStr + "'s king in "
                                                 + "check or " + colorOfPieceStr + "'s king is in check and this move "
                                                 + "doesn't fix that. Move can't be made.");
            }

            if (colorOfPiece == WHITE) {
                whiteKingHasMoved = true;
                whiteKingsRookHasMoved = true;
            } else {
                blackKingHasMoved = true;
                blackKingsRookHasMoved = true;
            }
        } else if (isCastlingQueenside) {
            if (!isCastlingPossible(colorOfPiece, QUEEN)) {
                throw new CastlingNotPossibleException("Castling queenside is not possible for " + colorOfPieceStr);
            }

            savedPieceIntNo1 = boardArray[5][rookYCoord];
            boardArray[5][rookYCoord] = boardArray[rookXCoord][rookYCoord];
            boardArray[rookXCoord][rookYCoord] = 0;

            savedPieceIntNo2 = boardArray[6][kingYCoord];
            boardArray[6][kingYCoord] = boardArray[kingXCoord][kingYCoord];
            boardArray[kingXCoord][kingYCoord] = 0;

            if (BoardArrays.isKingInCheck(boardArray, otherColor, colorOnTop)) {
                boardArray[kingXCoord][kingYCoord] = boardArray[6][kingYCoord];
                boardArray[6][kingYCoord] = savedPieceIntNo2;

                boardArray[rookXCoord][rookYCoord] = boardArray[5][rookYCoord];
                boardArray[5][rookYCoord] = savedPieceIntNo1;

                throw new KingIsInCheckException("Castling queenside would place " + colorOfPieceStr + "'s king in "
                                                 + "check or " + colorOfPieceStr + "'s king is in check and this move "
                                                 + "doesn't fix that. Move can't be made.");
            }
            if (colorOfPiece == WHITE) {
                whiteKingHasMoved = true;
                whiteQueensRookHasMoved = true;
            } else {
                blackKingHasMoved = true;
                blackQueensRookHasMoved = true;
            }
        }
    }

    private void movePieceNonCastling(final Chessboard.Move moveObj) throws KingIsInCheckException, IllegalArgumentException {
        int fromXCoord = moveObj.fromXCoord();
        int fromYCoord = moveObj.fromYCoord();
        int toXCoord = moveObj.toXCoord();
        int toYCoord = moveObj.toYCoord();
        int pieceInt = moveObj.movingPiece().pieceInt();
        int colorOfPiece = (pieceInt & WHITE) != 0 ? WHITE : BLACK;
        int otherColor = (colorOfPiece == WHITE) ? BLACK : WHITE;
        int capturedPieceInt = boardArray[toXCoord][toYCoord];
        String thisColorStr = (colorOfPiece == WHITE ? "White" : "Black");

        boardArray[toXCoord][toYCoord] = boardArray[fromXCoord][fromYCoord];
        boardArray[fromXCoord][fromYCoord] = 0;

        if (BoardArrays.isKingInCheck(boardArray, otherColor, colorOnTop)) {
            boardArray[fromXCoord][fromYCoord] = boardArray[toXCoord][toYCoord];
            boardArray[toXCoord][toYCoord] = capturedPieceInt;
            throw new KingIsInCheckException("Move would place " + thisColorStr + "'s king in check or " + thisColorStr
                                             + "'s King is in check and this move doesn't fix that; move can't be "
                                             + "made.");
        }

        switch (pieceInt) {
            case BLACK | KING -> {
                    if (!blackKingHasMoved) {
                        blackKingHasMoved = true;
                    }
                }
            case WHITE | KING -> {
                    if (!whiteKingHasMoved) {
                        whiteKingHasMoved = true;
                    }
                }
            case BLACK | ROOK -> {
                    if (!blackKingsRookHasMoved && fromXCoord == 0) {
                        blackKingsRookHasMoved = true;
                    } else if (!blackQueensRookHasMoved && fromXCoord == 7) {
                        blackQueensRookHasMoved = true;
                    }
                }
            case WHITE | ROOK -> {
                    if (!whiteKingsRookHasMoved && fromXCoord == 0) {
                        whiteKingsRookHasMoved = true;
                    } else if (!whiteQueensRookHasMoved && fromXCoord == 7) {
                        whiteQueensRookHasMoved = true;
                    }
                }
            default -> { }
        }
    }

    public void movePiece(final Chessboard.Move moveObj) throws KingIsInCheckException, IllegalArgumentException, CastlingNotPossibleException {
        if (moveObj.isCastlingKingside() || moveObj.isCastlingQueenside()) {
            movePieceCastling(moveObj);
        } else {
            movePieceNonCastling(moveObj);
        }
    }

    public int[][] occupiedSquareCoords() {
        int pieceCount = 0;
        int pieceIndex = 0;

        for (int xIdx = 0; xIdx < 8; xIdx++) {
            for (int yIdx = 0; yIdx < 8; yIdx++) {
                if (boardArray[xIdx][yIdx] != 0) {
                    pieceCount++;
                }
            }
        }

        int[][] squareCoords = new int[pieceCount][2];

        for (int xIdx = 0; xIdx < 8; xIdx++) {
            for (int yIdx = 0; yIdx < 8; yIdx++) {
                if (boardArray[xIdx][yIdx] != 0) {
                    squareCoords[pieceIndex][0] = xIdx;
                    squareCoords[pieceIndex][1] = yIdx;
                    pieceIndex++;
                }
            }
        }

        return squareCoords;
    }
}
