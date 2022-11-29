package com.kmfahey.jchessgame;

import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.Random;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;

public final class BoardArrays {

    public static final int LEFT =      0b0000000001;
    public static final int RIGHT =     0b0000000010;

    public static final int PAWN =      0b0000000100;
    public static final int ROOK =      0b0000001000;
    public static final int KNIGHT =    0b0000010000;
    public static final int BISHOP =    0b0000100000;
    public static final int QUEEN =     0b0001000000;
    public static final int KING =      0b0010000000;

    public static final int WHITE =     0b0100000000;
    public static final int BLACK =     0b1000000000;

    public static final int DOUBLED = 0;
    public static final int ISOLATED = 1;
    public static final int BLOCKED = 2;

    public static final int[] PAWN_PROMOTION_PIECES = new int[] {ROOK, KNIGHT, BISHOP, QUEEN};

    /* These are alternate versions of main arrays in the int-array-based board
       logic that are used by some methods. In order to avoid instantiating a
       new array each time one is called, a spare array is stored to an instance
       variable and reused each time that method is called. */
    private static final int[][] kingsMovesSpareBoardArray = new int[8][8];
    private static final int[][] tallyPawnsCoords = new int[8][2];
    private static final int[][] colorMobilitySpareMovesArray = new int[128][7];

    private static final HashMap<String, Double> evaluateBoardMemoizeMap = new HashMap<String, Double>();

    private BoardArrays() { };

    private static Random randomNumberGenerator = new Random();

    public static int[] findKing(final int[][] boardArray, final int kingColor) {
        int xIdx = -1;
        int yIdx = -1;
        for (xIdx = 0; xIdx < 8; xIdx++) {
            for (yIdx = 0; yIdx < 8; yIdx++) {
                if (boardArray[xIdx][yIdx] == (kingColor | KING)) {
                    return new int[] {xIdx, yIdx};
                }
            }
        }

        /* The loops completed without matching the King, so null is returned as
           an error value. */
        return null;
    }

    public static void shuffleMovesArray(int[][] movesArray, int usedLength) {
        /* This is Fisher–Yates shuffle I'm told. idk I just copied code off
           stackoverflow as is best practice :) */
        for (int startingIndex = usedLength - 1; startingIndex > 0; startingIndex--) {
            int randomIndex = randomNumberGenerator.nextInt(startingIndex + 1);
            // Simple swap
            int[] swapValue = movesArray[randomIndex];
            movesArray[randomIndex] = movesArray[startingIndex];
            movesArray[startingIndex] = swapValue;
        }
    }

    public static int generatePossibleMoves(final int[][] boardArray, final int[][] movesArray,
                                            final int colorsTurnItIs, final int colorOnTop
                                            ) throws AlgorithmBadArgumentException {
        int colorOpposing = (colorsTurnItIs == WHITE) ? BLACK : WHITE;
        int moveIdx = 0;
        boolean kingIsInCheck;

        kingIsInCheck = isKingInCheck(boardArray, colorOpposing, colorOnTop);

        for (int xIdx = 0; xIdx < 8; xIdx++) {
            for (int yIdx = 0; yIdx < 8; yIdx++) {
                int pieceInt = boardArray[xIdx][yIdx];
                if (pieceInt == 0 || (pieceInt & colorOpposing) != 0
                    /* If the king is in check the AI must move their king out
                       of check, so pieces other than the king are skipped. */
                    || kingIsInCheck && (pieceInt ^ colorsTurnItIs) != KING) {
                    continue;
                }
                moveIdx = generatePieceMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs, colorOnTop);
            }
        }

        return moveIdx;
    }

    public static int generatePieceMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdx,
                                  final int xIdx, final int yIdx, final int colorsTurnItIs, final int colorOnTop
                                  ) throws AlgorithmBadArgumentException {
        int pieceInt = boardArray[xIdx][yIdx];

        switch (pieceInt ^ colorsTurnItIs) {
            case PAWN:
                return generatePawnsMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs, colorOnTop);
            case ROOK:
                return generateRooksMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs);
            case KNIGHT | LEFT: case KNIGHT | RIGHT:
                return generateKnightsMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs);
            case BISHOP:
                return generateBishopsMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs);
            case QUEEN:
                return generateQueensMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs);
            case KING:
                return generateKingsMoves(boardArray, movesArray, moveIdx, xIdx, yIdx, colorsTurnItIs, colorOnTop);
            default:
                break;
        }

        return moveIdx;
    }

    public static int generatePawnsMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdxArg,
                                  final int xIdx, final int yIdx, final int colorsTurnItIs, final int colorOnTop
                                  ) throws AlgorithmBadArgumentException {
        int colorOnBottom = colorOnTop == WHITE ? BLACK : WHITE;
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;
        int pawnPieceInt;
        int xIdxMod;
        int yIdxMod;
        int moveIdx = moveIdxArg;

        pawnPieceInt = boardArray[xIdx][yIdx];

        if (pawnPieceInt != (colorsTurnItIs | PAWN)) {
            throw new AlgorithmBadArgumentException("generatePawnsMoves() called with coordinates that point to cell "
                                                    + "on board that's not a pawn or not the color whose turn it is");
        }

        if (yIdx < 7 && colorOnTop == colorsTurnItIs) {
            yIdxMod = yIdx + 1;
        } else if (yIdx > 0 && colorOnTop != colorsTurnItIs) {
            yIdxMod = yIdx - 1;
        } else {
            return moveIdx;
        }

        if (boardArray[xIdx][yIdxMod] == 0) {
            if (yIdxMod == 7 && colorsTurnItIs == colorOnTop) {
                for (int newPiece : PAWN_PROMOTION_PIECES) {
                    if (newPiece == KNIGHT) {
                        setMoveToMovesArray(
                            movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdxMod, 0,
                            (newPiece | colorsTurnItIs | (randomNumberGenerator.nextInt(2) == 1 ? LEFT : RIGHT))
                        );                              /* Since there's two different Knight models, LEFT and RIGHT, */
                    } else {                            /* the above expression chooses at random between them.       */
                        setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdxMod, 0,
                                            (newPiece | colorsTurnItIs));
                    }
                    moveIdx++;
                }
            } else if (yIdxMod == 0 && colorsTurnItIs == colorOnBottom) {
                for (int newPiece : PAWN_PROMOTION_PIECES) {
                    if (newPiece == KNIGHT) {
                        setMoveToMovesArray(
                            movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdxMod, 0,
                            (newPiece | colorsTurnItIs | (randomNumberGenerator.nextInt(2) == 1 ? LEFT : RIGHT))
                        );
                    } else {
                        setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdxMod, 0,
                                            (newPiece | colorsTurnItIs));
                    }
                    moveIdx++;
                }
            } else {
                setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdxMod, 0);
                moveIdx++;
            }
        }
        if (xIdx < 7 && (boardArray[xIdx + 1][yIdxMod] & otherColor) != 0) {
            xIdxMod = xIdx + 1;
            if (yIdxMod == 7 && colorsTurnItIs == colorOnTop) {
                for (int newPiece : PAWN_PROMOTION_PIECES) {
                    if (newPiece == KNIGHT) {
                        setMoveToMovesArray(
                            movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                            boardArray[xIdxMod][yIdxMod],
                            (newPiece | colorsTurnItIs | (randomNumberGenerator.nextInt(2) == 1 ? LEFT : RIGHT))
                        );
                    } else {
                        setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                            boardArray[xIdxMod][yIdxMod], (newPiece | colorsTurnItIs));
                    }

                    moveIdx++;
                }
            } else if (yIdxMod == 0 && colorsTurnItIs == colorOnBottom) {
                for (int newPiece : PAWN_PROMOTION_PIECES) {
                    if (newPiece == KNIGHT) {
                        setMoveToMovesArray(
                            movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                            boardArray[xIdxMod][yIdxMod],
                            (newPiece | colorsTurnItIs | (randomNumberGenerator.nextInt(2) == 1 ? LEFT : RIGHT))
                        );
                    } else {
                        setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                            boardArray[xIdxMod][yIdxMod], (newPiece | colorsTurnItIs));
                    }
                    moveIdx++;
                }
            } else {
                setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                moveIdx++;
            }
        }
        if (xIdx > 0 && (boardArray[xIdx - 1][yIdxMod] & otherColor) != 0) {
            xIdxMod = xIdx - 1;
            if (yIdxMod == 7 && colorsTurnItIs == colorOnTop) {
                for (int newPiece : PAWN_PROMOTION_PIECES) {
                    if (newPiece == KNIGHT) {
                        setMoveToMovesArray(
                            movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                            boardArray[xIdxMod][yIdxMod],
                            (newPiece | colorsTurnItIs | (randomNumberGenerator.nextInt(2) == 1 ? LEFT : RIGHT))
                        );
                    } else {
                        setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                            boardArray[xIdxMod][yIdxMod], (newPiece | colorsTurnItIs));
                    }
                    moveIdx++;
                }
            } else if (yIdxMod == 0 && colorsTurnItIs == colorOnBottom) {
                for (int newPiece : PAWN_PROMOTION_PIECES) {
                    if (newPiece == KNIGHT) {
                        setMoveToMovesArray(
                            movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                            boardArray[xIdxMod][yIdxMod],
                            (newPiece | colorsTurnItIs | (randomNumberGenerator.nextInt(2) == 1 ? LEFT : RIGHT))
                        );
                    } else {
                        setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                            boardArray[xIdxMod][yIdxMod], (newPiece | colorsTurnItIs));
                    }
                    moveIdx++;
                }
            } else {
                setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                moveIdx++;
            }
        }

        if (colorsTurnItIs == colorOnTop && yIdx == 1 && boardArray[xIdx][yIdx + 2] == 0) {
            setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdx + 2, 0);
            moveIdx++;
        } else if (colorsTurnItIs != colorOnTop && yIdx == 6 && boardArray[xIdx][yIdx - 2] == 0) {
            setMoveToMovesArray(movesArray, moveIdx, pawnPieceInt, xIdx, yIdx, xIdx, yIdx - 2, 0);
            moveIdx++;
        }

        return moveIdx;
    }

    public static int generateRooksMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdxArg,
                                  final int xIdx, final int yIdx, final int colorsTurnItIs
                                  ) throws AlgorithmBadArgumentException {
        int rookPieceInt;
        int moveIdx = moveIdxArg;

        rookPieceInt = boardArray[xIdx][yIdx];

        if (rookPieceInt != (colorsTurnItIs | ROOK)) {
            throw new AlgorithmBadArgumentException("generateRooksMoves() called with coordinates that point to cell "
                                                    + "on board that's not a rook or not the color whose turn it is");
        }

        if (yIdx < 7) {
            for (int yIdxMod = yIdx + 1;
                 yIdxMod < 8 && (boardArray[xIdx][yIdxMod] & colorsTurnItIs) == 0;
                 yIdxMod++, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, rookPieceInt, xIdx, yIdx, xIdx, yIdxMod,
                                    boardArray[xIdx][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (yIdx > 0) {
            for (int yIdxMod = yIdx - 1;
                 yIdxMod >= 0 && (boardArray[xIdx][yIdxMod] & colorsTurnItIs) == 0;
                 yIdxMod--, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, rookPieceInt, xIdx, yIdx, xIdx, yIdxMod,
                                    boardArray[xIdx][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (xIdx < 7) {
            for (int xIdxMod = xIdx + 1;
                 xIdxMod < 8 && (boardArray[xIdxMod][yIdx] & colorsTurnItIs) == 0;
                 xIdxMod++, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, rookPieceInt, xIdx, yIdx, xIdxMod, yIdx,
                                    boardArray[xIdxMod][yIdx]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (xIdx > 0) {
            for (int xIdxMod = xIdx - 1;
                 xIdxMod >= 0 && (boardArray[xIdxMod][yIdx] & colorsTurnItIs) == 0;
                 xIdxMod--, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, rookPieceInt, xIdx, yIdx, xIdxMod, yIdx,
                                    boardArray[xIdxMod][yIdx]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }

        return moveIdx;
    }

    public static int generateBishopsMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdxArg,
                                    final int xIdx, final int yIdx, final int colorsTurnItIs
                                    ) throws AlgorithmBadArgumentException {
        int bishopPieceInt;
        int moveIdx = moveIdxArg;

        bishopPieceInt = boardArray[xIdx][yIdx];

        if (bishopPieceInt != (colorsTurnItIs | BISHOP)) {
            throw new AlgorithmBadArgumentException("generateBishopsMoves() called with coordinates that point to "
                                                    + "cell on board that's not a bishop or not the color whose turn "
                                                    + "it is");
        }

        if (xIdx < 7 && yIdx < 7) {
            for (int xIdxMod = xIdx + 1, yIdxMod = yIdx + 1;
                xIdxMod < 8 && yIdxMod < 8 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                xIdxMod++, yIdxMod++, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, bishopPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (xIdx > 0 && yIdx < 7) {
            for (int xIdxMod = xIdx - 1, yIdxMod = yIdx + 1;
                xIdxMod >= 0 && yIdxMod < 8 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                xIdxMod--, yIdxMod++, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, bishopPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (xIdx < 7 && yIdx > 0) {
            for (int xIdxMod = xIdx + 1, yIdxMod = yIdx - 1;
                xIdxMod < 8 && yIdxMod >= 0 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                xIdxMod++, yIdxMod--, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, bishopPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (xIdx > 0 && yIdx > 0) {
            for (int xIdxMod = xIdx - 1, yIdxMod = yIdx - 1;
                xIdxMod >= 0 && yIdxMod >= 0 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                xIdxMod--, yIdxMod--, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, bishopPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }

        return moveIdx;
    }

    public static int generateKnightsMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdxArg,
                                    final int xIdx, final int yIdx, final int colorsTurnItIs
                                    ) throws AlgorithmBadArgumentException {
        int knightPieceInt;
        int moveIdx = moveIdxArg;

        knightPieceInt = boardArray[xIdx][yIdx];

        if (knightPieceInt != (colorsTurnItIs | KNIGHT | LEFT) && knightPieceInt != (colorsTurnItIs | KNIGHT | RIGHT)) {
            throw new AlgorithmBadArgumentException("generateKnightsMoves() called with coordinates that point to "
                                                    + "cell on board that's not a knight or not the color whose turn "
                                                    + "it is");
        }

        for (int xIdxDelta = -2; xIdxDelta <= 2; xIdxDelta++) {
            for (int yIdxDelta = -2; yIdxDelta <= 2; yIdxDelta++) {
                if (Math.abs(xIdxDelta) == Math.abs(yIdxDelta) || xIdxDelta == 0 || yIdxDelta == 0) {
                    continue;
                }

                int xIdxMod = xIdx + xIdxDelta;
                int yIdxMod = yIdx + yIdxDelta;

                if (xIdxMod > 7 || xIdxMod < 0 || yIdxMod > 7 || yIdxMod < 0
                    || (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) != 0) {
                    continue;
                }

                setMoveToMovesArray(movesArray, moveIdx, knightPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                    boardArray[xIdxMod][yIdxMod]);
                moveIdx++;
            }
        }

        return moveIdx;
    }

    public static int generateQueensMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdxArg,
                                   final int xIdx, final int yIdx, final int colorsTurnItIs
                                   ) throws AlgorithmBadArgumentException {
        int queenPieceInt;
        int moveIdx = moveIdxArg;

        queenPieceInt = boardArray[xIdx][yIdx];

        if (queenPieceInt != (colorsTurnItIs | QUEEN)) {
            throw new AlgorithmBadArgumentException("generateQueensMoves() called with coordinates that point to cell "
                                                    + "on board that's not a queen or not the color whose turn it is");
        }

        if (xIdx < 7) {
            for (int xIdxMod = xIdx + 1;
                xIdxMod < 8 && (boardArray[xIdxMod][yIdx] & colorsTurnItIs) == 0;
                xIdxMod++, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdxMod, yIdx,
                                    boardArray[xIdxMod][yIdx]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
            if (yIdx < 7) {
                for (int xIdxMod = xIdx + 1, yIdxMod = yIdx + 1;
                     xIdxMod < 8 && yIdxMod < 8 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                     xIdxMod++, yIdxMod++, moveIdx++) {
                    setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                        boardArray[xIdxMod][yIdxMod]);
                    if (movesArray[moveIdx][5] != 0) {
                        moveIdx++;
                        break;
                    }
                }
            }
            if (yIdx > 0) {
                for (int xIdxMod = xIdx + 1, yIdxMod = yIdx - 1; xIdxMod < 8 && yIdxMod >= 0
                     && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0; xIdxMod++, yIdxMod--, moveIdx++) {
                    setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                        boardArray[xIdxMod][yIdxMod]);
                    if (movesArray[moveIdx][5] != 0) {
                        moveIdx++;
                        break;
                    }
                }
            }
        }
        if (yIdx < 7) {
            for (int yIdxMod = yIdx + 1;
                 yIdxMod < 8 && (boardArray[xIdx][yIdxMod] & colorsTurnItIs) == 0;
                 yIdxMod++, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdx, yIdxMod,
                                    boardArray[xIdx][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (yIdx > 0) {
            for (int yIdxMod = yIdx - 1;
                 yIdxMod >= 0 && (boardArray[xIdx][yIdxMod] & colorsTurnItIs) == 0;
                 yIdxMod--, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdx, yIdxMod,
                                    boardArray[xIdx][yIdxMod]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
        }
        if (xIdx > 0) {
            for (int xIdxMod = xIdx - 1;
                 xIdxMod >= 0 && (boardArray[xIdxMod][yIdx] & colorsTurnItIs) == 0;
                 xIdxMod--, moveIdx++) {
                setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdxMod, yIdx,
                                    boardArray[xIdxMod][yIdx]);
                if (movesArray[moveIdx][5] != 0) {
                    moveIdx++;
                    break;
                }
            }
            if (yIdx < 7) {
                for (int xIdxMod = xIdx - 1, yIdxMod = yIdx + 1;
                     xIdxMod >= 0 && yIdxMod < 8 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                     xIdxMod--, yIdxMod++, moveIdx++) {
                    setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                        boardArray[xIdxMod][yIdxMod]);
                    if (movesArray[moveIdx][5] != 0) {
                        moveIdx++;
                        break;
                    }
                }
            }
            if (yIdx > 0) {
                for (int xIdxMod = xIdx - 1, yIdxMod = yIdx - 1;
                     xIdxMod >= 0 && yIdxMod >= 0 && (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) == 0;
                     xIdxMod--, yIdxMod--, moveIdx++) {
                    setMoveToMovesArray(movesArray, moveIdx, queenPieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                        boardArray[xIdxMod][yIdxMod]);
                    if (movesArray[moveIdx][5] != 0) {
                        moveIdx++;
                        break;
                    }
                }
            }
        }

        return moveIdx;
    }

    public static int generateKingsMoves(final int[][] boardArray, final int[][] movesArray, final int moveIdxArg,
                                         final int xIdx, final int yIdx, final int colorsTurnItIs, final int colorOnTop
                                         ) throws AlgorithmBadArgumentException {
        int colorThreatening = (colorsTurnItIs == WHITE) ? BLACK : WHITE;
        int pieceInt;
        int atLocPieceInt;
        int kingPieceInt;
        int moveIdx = moveIdxArg;
        boolean kingIsInCheckHere;

        pieceInt = boardArray[xIdx][yIdx];

        if (pieceInt != (colorsTurnItIs | KING)) {
            throw new AlgorithmBadArgumentException("generateKingsMoves() called with coordinates that point to cell "
                                                    + "on board that's not a king or not the color whose turn it is");
        }

        for (int otherXIdx = 0; otherXIdx < 8; otherXIdx++) {
            for (int otherYIdx = 0; otherYIdx < 8; otherYIdx++) {
                kingsMovesSpareBoardArray[otherXIdx][otherYIdx] = boardArray[otherXIdx][otherYIdx];
            }
        }

        kingPieceInt = kingsMovesSpareBoardArray[xIdx][yIdx];
        kingsMovesSpareBoardArray[xIdx][yIdx] = 0;

        for (int xIdxDelta = -1; xIdxDelta <= 1; xIdxDelta++) {
            for (int yIdxDelta = -1; yIdxDelta <= 1; yIdxDelta++) {
                if (xIdxDelta == 0 && yIdxDelta == 0) {
                    continue;
                }
                int xIdxMod = xIdx + xIdxDelta;
                int yIdxMod = yIdx + yIdxDelta;
                if (xIdxMod < 0 || xIdxMod > 7 || yIdxMod < 0 || yIdxMod > 7
                    || (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) != 0) {
                    continue;
                }

                atLocPieceInt = kingsMovesSpareBoardArray[xIdxMod][yIdxMod];
                kingsMovesSpareBoardArray[xIdxMod][yIdxMod] = kingPieceInt;
                kingIsInCheckHere = isKingInCheck(kingsMovesSpareBoardArray, colorThreatening, colorOnTop);
                kingsMovesSpareBoardArray[xIdxMod][yIdxMod] = atLocPieceInt;

                if (kingIsInCheckHere) {
                    continue;
                }

                if (movesArray != null) {
                    setMoveToMovesArray(movesArray, moveIdx, pieceInt, xIdx, yIdx, xIdxMod, yIdxMod,
                                        boardArray[xIdxMod][yIdxMod]);
                }
                moveIdx++;
            }
        }

        if (yIdx == 0 || yIdx == 7 && xIdx == 3) {
            if (boardArray[2][yIdx] == 0 && boardArray[1][yIdx] == 0
                && (boardArray[0][yIdx] == (colorsTurnItIs | ROOK))) {
                setMoveToMovesArray(movesArray, moveIdx, pieceInt, xIdx, yIdx, 0, yIdx, boardArray[0][0]);
                moveIdx++;
            } else if (boardArray[4][yIdx] == 0 && boardArray[5][yIdx] == 0 && boardArray[6][yIdx] == 0
                       && (boardArray[7][yIdx] == (colorsTurnItIs | ROOK))) {
                setMoveToMovesArray(movesArray, moveIdx, pieceInt, xIdx, yIdx, 7, yIdx, boardArray[7][yIdx]);
                moveIdx++;
            }
        }

        return moveIdx;
    }

    private static void setMoveToMovesArray(final int[][] movesArray, final int moveIdx, final int pieceInt,
                                     final int startXIdx, final int startYIdx, final int endXIdx, final int endYIdx,
                                     final int capturedPiece) throws AlgorithmBadArgumentException {
        setMoveToMovesArray(movesArray, moveIdx, pieceInt, startXIdx, startYIdx, endXIdx, endYIdx, capturedPiece, 0);
    }

    private static void setMoveToMovesArray(final int[][] movesArray, final int moveIdx, final int pieceInt,
                                     final int startXIdx, final int startYIdx, final int endXIdx, final int endYIdx,
                                     final int capturedPiece, final int promotedToPieceInt
                                     ) throws AlgorithmBadArgumentException {
        if (movesArray[moveIdx][0] != 0) {
            throw new AlgorithmBadArgumentException("setMoveToMovesArray() called with moveIdx arg pointing to "
                                                    + "non-zero entry in movesArray argument");
        }
        movesArray[moveIdx][0] = pieceInt;
        movesArray[moveIdx][1] = startXIdx;
        movesArray[moveIdx][2] = startYIdx;
        movesArray[moveIdx][3] = endXIdx;
        movesArray[moveIdx][4] = endYIdx;
        movesArray[moveIdx][5] = capturedPiece;
        movesArray[moveIdx][6] = promotedToPieceInt;
    }

    public static boolean isKingInCheck(final int[][] boardArray, final int colorThreatening, final int colorOnTop) {
        int colorOfKing = (colorThreatening == WHITE) ? BLACK : WHITE;
        boolean kingIsInCheck = false;
        threatsch:
        for (int xIdx = 0; xIdx < 8; xIdx++) {
            for (int yIdx = 0; yIdx < 8; yIdx++) {
                int pieceInt = boardArray[xIdx][yIdx];
                if (pieceInt == 0 || (pieceInt & colorOfKing) != 0) {
                    continue;
                }
                switch (pieceInt ^ colorThreatening) {
                    case PAWN:
                        kingIsInCheck = doesPawnHaveKingInCheck(boardArray, xIdx, yIdx, colorThreatening, colorOnTop);
                        break;
                    case ROOK:
                        kingIsInCheck = doesRookHaveKingInCheck(boardArray, xIdx, yIdx, colorThreatening);
                        break;
                    case KNIGHT | LEFT: case KNIGHT | RIGHT:
                        kingIsInCheck = doesKnightHaveKingInCheck(boardArray, xIdx, yIdx, colorThreatening);
                        break;
                    case BISHOP:
                        kingIsInCheck = doesBishopHaveKingInCheck(boardArray, xIdx, yIdx, colorThreatening);
                        break;
                    case QUEEN:
                        kingIsInCheck = doesQueenHaveKingInCheck(boardArray, xIdx, yIdx, colorThreatening);
                        break;
                    case KING:
                        kingIsInCheck = doesKingHaveKingInCheck(boardArray, xIdx, yIdx, colorThreatening);
                        break;
                    default:
                        break;
                }
                if (kingIsInCheck) {
                    break threatsch;
                }
            }
        }

        return kingIsInCheck;
    }

    public static boolean doesPawnHaveKingInCheck(final int[][] boardArray, final int xIdx,
                                            final int yIdx, final int colorsTurnItIs, final int colorOnTop) {
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;
        int yIdxMod;

        if (yIdx < 7 && (colorOnTop == WHITE && colorsTurnItIs == WHITE
            || colorOnTop == BLACK && colorsTurnItIs == BLACK)) {
            yIdxMod = yIdx + 1;
        } else if (yIdx > 0 && (colorOnTop == BLACK && colorsTurnItIs == WHITE
            || colorOnTop == WHITE && colorsTurnItIs == BLACK)) {
            yIdxMod = yIdx - 1;
        } else {
            return false;
        }

        for (int xIdxMod = xIdx - 1; xIdxMod <= xIdx + 1; xIdxMod += 2) {
            if (xIdxMod < 0 || xIdxMod > 7) {
                continue;
            }

            if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                return true;
            } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                break;
            }
        }

        return false;
    }

    public static boolean doesRookHaveKingInCheck(final int[][] boardArray, final int xIdx, final int yIdx,
                                            final int colorsTurnItIs) {
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;

        if (yIdx < 7) {
            for (int yIdxMod = yIdx + 1; yIdxMod < 8; yIdxMod++) {
                if (boardArray[xIdx][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdx][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (yIdx > 0) {
            for (int yIdxMod = yIdx - 1; yIdxMod >= 0; yIdxMod--) {
                if (boardArray[xIdx][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdx][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (xIdx < 7) {
            for (int xIdxMod = xIdx + 1; xIdxMod < 8; xIdxMod++) {
                if (boardArray[xIdxMod][yIdx] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdx] != 0) {
                    break;
                }
            }
        }
        if (xIdx > 0) {
            for (int xIdxMod = xIdx - 1; xIdxMod >= 0; xIdxMod--) {
                if (boardArray[xIdxMod][yIdx] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdx] != 0) {
                    break;
                }
            }
        }

        return false;
    }

    public static boolean doesKnightHaveKingInCheck(final int[][] boardArray, final int xIdx, final int yIdx,
                                              final int colorsTurnItIs) {
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;

        for (int xIdxDelta = -2; xIdxDelta <= 2; xIdxDelta++) {
            for (int yIdxDelta = -2; yIdxDelta <= 2; yIdxDelta++) {
                if (Math.abs(xIdxDelta) == Math.abs(yIdxDelta) || xIdxDelta == 0 || yIdxDelta == 0) {
                    continue;
                }

                int xIdxMod = xIdx + xIdxDelta;
                int yIdxMod = yIdx + yIdxDelta;

                if (xIdxMod > 7 || xIdxMod < 0 || yIdxMod > 7 || yIdxMod < 0
                    || (boardArray[xIdxMod][yIdxMod] & colorsTurnItIs) != 0) {
                    continue;
                }

                if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                    break;
                }
            }
        }

        return false;
    }

    public static boolean doesBishopHaveKingInCheck(final int[][] boardArray, final int xIdx, final int yIdx,
                                              final int colorsTurnItIs) {
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;

        if (xIdx < 7 && yIdx < 7) {
            for (int xIdxMod = xIdx + 1, yIdxMod = yIdx + 1; xIdxMod < 8 && yIdxMod < 8; xIdxMod++, yIdxMod++) {
                if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (xIdx > 0 && yIdx < 7) {
            for (int xIdxMod = xIdx - 1, yIdxMod = yIdx + 1; xIdxMod >= 0 && yIdxMod < 8; xIdxMod--, yIdxMod++) {
                if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (xIdx < 7 && yIdx > 0) {
            for (int xIdxMod = xIdx + 1, yIdxMod = yIdx - 1; xIdxMod < 8 && yIdxMod >= 0; xIdxMod++, yIdxMod--) {
                if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (xIdx > 0 && yIdx > 0) {
            for (int xIdxMod = xIdx - 1, yIdxMod = yIdx - 1; xIdxMod >= 0 && yIdxMod >= 0; xIdxMod--, yIdxMod--) {
                if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                    break;
                }
            }
        }

        return false;
    }

    public static boolean doesQueenHaveKingInCheck(final int[][] boardArray, final int xIdx, final int yIdx,
                                             final int colorsTurnItIs) {
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;

        if (xIdx < 7) {
            for (int xIdxMod = xIdx + 1; xIdxMod < 8; xIdxMod++) {
                if (boardArray[xIdxMod][yIdx] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdx] != 0) {
                    break;
                }
            }
            if (yIdx < 7) {
                for (int xIdxMod = xIdx + 1, yIdxMod = yIdx + 1; xIdxMod < 8 && yIdxMod < 8; xIdxMod++, yIdxMod++) {
                    if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                        return true;
                    } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                        break;
                    }
                }
            }
            if (yIdx > 0) {
                for (int xIdxMod = xIdx + 1, yIdxMod = yIdx - 1; xIdxMod < 8 && yIdxMod >= 0; xIdxMod++, yIdxMod--) {
                    if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                        return true;
                    } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                        break;
                    }
                }
            }
        }
        if (yIdx < 7) {
            for (int yIdxMod = yIdx + 1; yIdxMod < 8; yIdxMod++) {
                if (boardArray[xIdx][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdx][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (yIdx > 0) {
            for (int yIdxMod = yIdx - 1; yIdxMod >= 0; yIdxMod--) {
                if (boardArray[xIdx][yIdxMod] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdx][yIdxMod] != 0) {
                    break;
                }
            }
        }
        if (xIdx > 0) {
            for (int xIdxMod = xIdx - 1; xIdxMod >= 0; xIdxMod--) {
                if (boardArray[xIdxMod][yIdx] == (otherColor | KING)) {
                    return true;
                } else if (boardArray[xIdxMod][yIdx] != 0) {
                    break;
                }
            }
            if (yIdx < 7) {
                for (int xIdxMod = xIdx - 1, yIdxMod = yIdx + 1; xIdxMod >= 0 && yIdxMod < 8; xIdxMod--, yIdxMod++) {
                    if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                        return true;
                    } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                        break;
                    }
                }
            }
            if (yIdx > 0) {
                for (int xIdxMod = xIdx - 1, yIdxMod = yIdx - 1; xIdxMod >= 0 && yIdxMod >= 0; xIdxMod--, yIdxMod--) {
                    if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                        return true;
                    } else if (boardArray[xIdxMod][yIdxMod] != 0) {
                        break;
                    }
                }
            }
        }

        return false;
    }

    public static boolean doesKingHaveKingInCheck(final int[][] boardArray, final int xIdx, final int yIdx,
                                                  final int colorsTurnItIs) {
        /* This really shouldn't happen at all, it's illegal for a king to put
           a king in check. But the AI's move-analysis depends on checking for
           in-check moves, so this needs to exist so the AI avoids trying to
           capture the player's king. */
        int otherColor = (colorsTurnItIs == WHITE) ? BLACK : WHITE;
        int xIdxMod;
        int yIdxMod;

        if (xIdx > 0 && yIdx > 0) {
            xIdxMod = xIdx - 1;
            yIdxMod = yIdx - 1;
            if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                return true;
            }
        }
        if (xIdx > 0) {
            xIdxMod = xIdx - 1;
            if (boardArray[xIdxMod][yIdx] == (otherColor | KING)) {
                return true;
            }
        }
        if (xIdx > 0 && yIdx < 7) {
            xIdxMod = xIdx - 1;
            yIdxMod = yIdx + 1;
            if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                return true;
            }
        }
        if (yIdx > 0) {
            yIdxMod = yIdx - 1;
            if (boardArray[xIdx][yIdxMod] == (otherColor | KING)) {
                return true;
            }
        }
        if (yIdx < 7) {
            yIdxMod = yIdx + 1;
            if (boardArray[xIdx][yIdxMod] == (otherColor | KING)) {
                return true;
            }
        }
        if (xIdx < 7 && yIdx > 0) {
            xIdxMod = xIdx + 1;
            yIdxMod = yIdx - 1;
            if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                return true;
            }
        }
        if (xIdx < 7) {
            xIdxMod = xIdx + 1;
            if (boardArray[xIdxMod][yIdx] == (otherColor | KING)) {
                return true;
            }
        }
        if (xIdx < 7 && yIdx < 7) {
            xIdxMod = xIdx + 1;
            yIdxMod = yIdx + 1;
            if (boardArray[xIdxMod][yIdxMod] == (otherColor | KING)) {
                return true;
            }
        }

        return false;
    }

    public static double[] tallySpecialPawns(final int[][] boardArray, final int colorInQuestion,
                                             final int colorOnTop) {
        int[][] doubledPawnsCoords = new int[8][2];
        double[] retval = new double[3];
        double doubledPawnsCount = 0;
        double isolatedPawnsCount = 0;
        double blockedPawnsCount = 0;
        int pawnsCount;
        int X_IDX = 0;
        int Y_IDX = 1;
        int dblpIdx = 0;
        int maxPawnIndex;

        pawnsCount = 0;

        for (int xIdx = 0, pawnIndex = 0; xIdx < 8; xIdx++) {
            for (int yIdx = 0; yIdx < 8; yIdx++) {
                if (boardArray[xIdx][yIdx] == (colorInQuestion | PAWN)) {
                    tallyPawnsCoords[pawnIndex][0] = xIdx;
                    tallyPawnsCoords[pawnIndex][1] = yIdx;
                    pawnIndex++;
                    pawnsCount++;
                }
            }
        }

        maxPawnIndex = pawnsCount - 1;

        /* Normally an isolated pawn is one where its neighboring pawns are each
           more than one file away from them. */
        switch (pawnsCount) {
            case 1 -> {
                /* But if there's only 1 pawn left it is by default isolated. */
                isolatedPawnsCount++;
            }
            case 2 -> {
                /* If there's two left, they're both isolated if their X indexes
                   differ by more than 1. */
                if (tallyPawnsCoords[1][X_IDX] - tallyPawnsCoords[0][X_IDX] > 1) {
                    isolatedPawnsCount += 2;
                }
            }
            default -> {
                for (int pawnIndex = 0; pawnIndex < pawnsCount; pawnIndex++) {
                    int thisPawnXIdx = tallyPawnsCoords[pawnIndex][X_IDX];

                    /* If a pawn is the leftmost on the board, it's isolated
                       if the difference between its X index and its right
                       neighbor's X index is more than 1. */
                    if (pawnIndex == 0) {
                        int nextPawnXIdx = tallyPawnsCoords[pawnIndex + 1][X_IDX];
                        if (nextPawnXIdx - thisPawnXIdx > 1) {
                            isolatedPawnsCount++;
                        }
                    /* Vice versa, if it's rightmost on the board, it's isolated
                       if its left neighbor's X index exceeds its X index by
                       more than 1. */
                    } else if (pawnIndex == maxPawnIndex) {
                        int prevPawnXIdx = tallyPawnsCoords[pawnIndex - 1][X_IDX];
                        if (prevPawnXIdx - thisPawnXIdx > 1) {
                            isolatedPawnsCount++;
                        }
                    } else if (0 <= pawnIndex - 1 && pawnIndex + 1 <= maxPawnIndex) {
                        int prevPawnXIdx = tallyPawnsCoords[pawnIndex - 1][X_IDX];
                        int nextPawnXIdx = tallyPawnsCoords[pawnIndex + 1][X_IDX];
                        if (nextPawnXIdx - thisPawnXIdx > 1 && thisPawnXIdx - prevPawnXIdx > 1) {
                            isolatedPawnsCount++;
                        }
                    }
                }
            }
        }

        for (int pawnIndex = 0; pawnIndex < pawnsCount; pawnIndex++) {
            int thisPawnXIdx = tallyPawnsCoords[pawnIndex][X_IDX];
            int thisPawnYIdx = tallyPawnsCoords[pawnIndex][Y_IDX];
            if (pawnIndex > 0) {
                int prevPawnXIdx = tallyPawnsCoords[pawnIndex - 1][X_IDX];
                int prevPawnYIdx = tallyPawnsCoords[pawnIndex - 1][Y_IDX];

                /* The y values in this loop's calculations run two different
                   ways depending on whether colorInQuestion is playing from
                   the top of the board (== instance var colorOnTop) or from
                   the bottom (!= colorOnTop). If on top, yDiff is set to -1,
                   otherwise +1. A successive piece's y index is calculated by
                   adding yDiff to the earlier piece's y index. */
                int yDiff = colorInQuestion == colorOnTop ? -1 : +1;

                /* Two successive pawns are doubled if their x indexes are equal
                   and the difference between the first y index and the second
                   equals yDiff (-1 if playing from the top of the board, +1 if
                   from the bottom). */
                if (thisPawnXIdx == prevPawnXIdx && thisPawnYIdx - prevPawnYIdx == yDiff) {

                    /* Tripled pawns are a possibility. If two pawns'
                       coordinates have already been stored to doubledPawnsCoord
                       (ie. dblpIdx > 1), its last coords are checked for
                       equality with the first doubled pawn. If they match, only
                       the second doubled pawn is counted and stored. Otherwise,
                       both are counted and stored. */
                    if (dblpIdx > 1 && doubledPawnsCoords[dblpIdx - 1][X_IDX] == prevPawnXIdx
                                    && doubledPawnsCoords[dblpIdx - 1][Y_IDX] == prevPawnYIdx) {
                        doubledPawnsCoords[dblpIdx][X_IDX] = thisPawnXIdx;
                        doubledPawnsCoords[dblpIdx][Y_IDX] = thisPawnYIdx;
                        dblpIdx++;
                        doubledPawnsCount++;
                    } else {
                        doubledPawnsCoords[dblpIdx][X_IDX] = prevPawnXIdx;
                        doubledPawnsCoords[dblpIdx][Y_IDX] = prevPawnYIdx;
                        dblpIdx++;
                        doubledPawnsCount++;
                        doubledPawnsCoords[dblpIdx][X_IDX] = thisPawnXIdx;
                        doubledPawnsCoords[dblpIdx][Y_IDX] = thisPawnYIdx;
                        dblpIdx++;
                        doubledPawnsCount++;
                    }
                }

                int nextSquareXIdx = thisPawnXIdx;
                int nextSquareYIdx = thisPawnYIdx + yDiff;
                int nextSquarePieceInt = boardArray[nextSquareXIdx][nextSquareYIdx];
                /* If the square ahead of this square is occupied, and it's
                   not a pawn on this side, then this pawn is blocked and
                   blockedPawnsCount is incremented. */
                if (nextSquarePieceInt != 0 && (nextSquarePieceInt ^ colorInQuestion) != PAWN) {
                    blockedPawnsCount++;
                }
            }
        }

        /* This method returns three doubles, so they're packed into a length-3
           array and that's returned. */
        retval[DOUBLED] = doubledPawnsCount;
        retval[BLOCKED] = blockedPawnsCount;
        retval[ISOLATED] = isolatedPawnsCount;
        return retval;
    }

    public static void printBoard(final int[][] boardArray) {
        StringJoiner outerJoiner = new StringJoiner(",\n", "new int[][] {\n", "\n}\n");
        for (int outerIndex = 0; outerIndex < 8; outerIndex++) {
            StringJoiner innerJoiner = new StringJoiner(", ", "new int[] {", "}");
            for (int innerIndex = 0; innerIndex < 8; innerIndex++) {
                innerJoiner.add(String.valueOf(boardArray[outerIndex][innerIndex]));
            }
            outerJoiner.add(innerJoiner.toString());
        }
        System.out.println(outerJoiner.toString());
    }

    public static boolean arrayOfCoordsContainsCoord(final int[][] arrayOfCoords, final int[] toFindCoords) {
        for (int[] arrayCoords : arrayOfCoords) {
            if (Arrays.equals(arrayCoords, toFindCoords)) {
                return true;
            }
        }

        return false;
    }

    public static int[][] fileNameToBoardArray(final String fileName
                                              ) throws NullPointerException, BoardArrayFileParsingException,
                                                       IOException {
        File boardFile = new File(fileName);
        String fileContents = Files.readString(boardFile.toPath());
        String[] fileLines = fileContents.split("\n");
        if (fileLines.length != 8) {
            throw new BoardArrayFileParsingException("Board file " + fileName + " doesn't have exactly 8 lines.");
        }
        int[][] boardArray = new int[8][8];
        for (int xIdx = 0; xIdx < 8; xIdx++) {
            String fileLine = fileLines[xIdx];
            String[] lineIntStr = fileLine.split(", *");
            if (lineIntStr.length != 8) {
                StringJoiner joiner = new StringJoiner(", ", "[", "]");
                for (String elem : lineIntStr) {
                    joiner.add(elem);
                }
                throw new BoardArrayFileParsingException("Board file " + fileName + ", line " + (xIdx + 1) + " doesn't"
                                                         + " have exactly 8 comma-separated values. (Found `"
                                                         + joiner.toString() + "` instead.)");
            }
            for (int yIdx = 0; yIdx < 8; yIdx++) {
                int pieceInt;
                try {
                    pieceInt = Integer.valueOf(lineIntStr[yIdx]);
                } catch (NumberFormatException exception) {
                    throw new BoardArrayFileParsingException("Board file " + fileName + ", line " + (xIdx + 1) + ", "
                                                             + "item " + (yIdx + 1) + " doesn't eval as an integer. "
                                                             + "(Found `" + lineIntStr[yIdx] + "` instead.)",
                                                             exception);
                }
                if (pieceInt == 0) {
                    boardArray[xIdx][yIdx] = pieceInt;
                } else if (!Chessboard.VALID_PIECE_INTS.contains(pieceInt)) {
                    throw new BoardArrayFileParsingException("Board file " + fileName + ", line " + (xIdx + 1) + ", "
                                                             + "item " + (yIdx + 1) + " is an integer that's not a "
                                                             + "valid piece representation value. (Value is `"
                                                             + pieceInt + "`.)");
                } else {
                    boardArray[xIdx][yIdx] = pieceInt;
                }
            }
        }

        if (Objects.isNull(findKing(boardArray, WHITE))) {
            throw new BoardArrayFileParsingException("The imported board doesn't contain a White king. "
                                                     + "Board is invalid.");
        } else if (Objects.isNull(findKing(boardArray, BLACK))) {
            throw new BoardArrayFileParsingException("The imported board doesn't contain a Black king. "
                                                     + "Board is invalid.");
        } else {
            return boardArray;
        }
    }
}
