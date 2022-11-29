package com.kmfahey.jchessgame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.StringJoiner;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import javax.swing.JFrame;

public class BoardView extends JComponent implements MouseListener, ActionListener {

    private static final int[][] LIGHT_COLORED_SQUARES_COORDS = new int[][] {
        new int[] {0, 0}, new int[] {0, 2}, new int[] {0, 4}, new int[] {0, 6},
        new int[] {1, 1}, new int[] {1, 3}, new int[] {1, 5}, new int[] {1, 7},
        new int[] {2, 0}, new int[] {2, 2}, new int[] {2, 4}, new int[] {2, 6},
        new int[] {3, 1}, new int[] {3, 3}, new int[] {3, 5}, new int[] {3, 7},
        new int[] {4, 0}, new int[] {4, 2}, new int[] {4, 4}, new int[] {4, 6},
        new int[] {5, 1}, new int[] {5, 3}, new int[] {5, 5}, new int[] {5, 7},
        new int[] {6, 0}, new int[] {6, 2}, new int[] {6, 4}, new int[] {6, 6},
        new int[] {7, 1}, new int[] {7, 3}, new int[] {7, 5}, new int[] {7, 7}
    };

    private static final Color BEIGE = new Color(0.949019F, 0.901960F, 0.800000F);

    private Dimension boardDims;
    private ImagesManager imagesManager;
    private CoordinatesManager coordinatesManager;
    private Chessboard chessboard;

    private Chessboard.Piece clickEventClickedPiece = null;
    private int[] clickEventMovingFrom = new int[] {-1, -1};
    private Chessboard.Piece clickEventToCapturePiece = null;
    private int[] clickEventMovingTo = new int[] {-1, -1};
    private boolean pawnHasntBeenPromotedYet = false;

    private Chessboard.Piece lastPieceMovedByPlayer;
    private JFrame chessGameFrame;

    private int colorOfPlayer;
    private int colorOfAI;
    private final int timerDelayMlsec = 500;
    private MinimaxRunner minimaxRunner;

    private Timer opposingMoveDelayTimer;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");

    public BoardView(final JFrame chessGame, final Dimension cmpntDims, final ImagesManager imgMgr,
                     final CoordinatesManager coordMgr, final Chessboard chessboardObj,
                     final int colorPlaying) {
        chessGameFrame = chessGame;
        boardDims = cmpntDims;
        imagesManager = imgMgr;
        coordinatesManager = coordMgr;
        chessboard = chessboardObj;
        colorOfPlayer = colorPlaying;
        colorOfAI = colorPlaying == BoardArrays.WHITE ? BoardArrays.BLACK : BoardArrays.WHITE;
        minimaxRunner = new MinimaxRunner(chessboard, colorOfAI);
        repaint();
    }

    public void promotePawn(int xCoord, int yCoord, int newPiece) {
        chessboard.promotePawn(xCoord, yCoord, newPiece);
        pawnHasntBeenPromotedYet = false;
    }

    @Override
    protected void paintComponent(final Graphics graphics) {
        int[][] squareCoords;
        int[][] boardArray;
        super.paintComponent(graphics);

        Dimension totalBoardDimensions = coordinatesManager.getTotalBoardDimensions();
        Insets beigeMarginInsets = coordinatesManager.getBeigeMarginInsets();
        Dimension beigeMarginDimensions = coordinatesManager.getBeigeMarginDimensions();
        Insets innerBlackBorderInsets = coordinatesManager.getInnerBlackBorderInsets();
        Dimension innerBlackBorderDimensions = coordinatesManager.getInnerBlackBorderDimensions();
        Dimension squareDimensions = coordinatesManager.getSquareDimensions();

        graphics.setColor(Color.BLACK);

        graphics.fillRect(0, 0, (int) totalBoardDimensions.getWidth(),
                                (int) totalBoardDimensions.getHeight());

        graphics.setColor(BEIGE);

        graphics.fillRect(beigeMarginInsets.left, beigeMarginInsets.top,
                          (int) beigeMarginDimensions.getWidth(),
                          (int) beigeMarginDimensions.getHeight());

        graphics.setColor(Color.BLACK);

        graphics.fillRect(innerBlackBorderInsets.left, innerBlackBorderInsets.top,
                          (int) innerBlackBorderDimensions.getWidth(),
                          (int) innerBlackBorderDimensions.getHeight());

        graphics.setColor(BEIGE);

        for (int[] lightColoredSquareCoords : LIGHT_COLORED_SQUARES_COORDS) {
            int xCoord = lightColoredSquareCoords[0];
            int yCoord = lightColoredSquareCoords[1];
            Point squareUpperLeftCorner = coordinatesManager
                                          .getSquareUpperLeftCorner(xCoord, yCoord);
            graphics.fillRect(squareUpperLeftCorner.x, squareUpperLeftCorner.y,
                              (int) squareDimensions.getWidth(),
                              (int) squareDimensions.getHeight());
        }

        boardArray = chessboard.getBoardArray();

        squareCoords = chessboard.occupiedSquareCoords();

        for (int coordsIdx = 0; coordsIdx < squareCoords.length; coordsIdx++) {
            int[] pieceCoords = squareCoords[coordsIdx];
            Chessboard.Piece piece = chessboard.getPieceAtCoords(pieceCoords[0], pieceCoords[1]);
            Image pieceIcon = piece.pieceImage();
            Point pieceUpperLeftCorner = coordinatesManager
                                         .getSquareUpperLeftCorner(piece.xCoord(), piece.yCoord());
            graphics.drawImage(pieceIcon, pieceUpperLeftCorner.x, pieceUpperLeftCorner.y, this);
        }
    }

    public void mouseClicked(final MouseEvent event) {
        PawnPromotionDBox pawnPrmtDBox = null;
        int eventXCoord = event.getX();
        int eventYCoord = event.getY();

        /* FIXME: this code needs to handle check and checkmate cases. */

        Insets boardSquareFieldInsets = coordinatesManager.getBoardSquareFieldInsets();
        Dimension squareDimensions = coordinatesManager.getSquareDimensions();
        Dimension boardSquareFieldDimensions = coordinatesManager.getBoardSquareFieldDimensions();

        if (eventXCoord <= boardSquareFieldInsets.left
            || eventXCoord > boardSquareFieldInsets.left + boardSquareFieldDimensions.getWidth()
            || eventYCoord <= boardSquareFieldInsets.top
            || eventYCoord > boardSquareFieldInsets.top + boardSquareFieldDimensions.getHeight()) {
            return;
        }
        int[] clickSquareCoord;
        int tempXCoord = eventXCoord;
        int tempYCoord = eventYCoord;
        int xCoord = 0;
        int yCoord = 0;

        while (tempXCoord > squareDimensions.getWidth()) {
            tempXCoord -= squareDimensions.getWidth();
            xCoord += 1;
        }
        while (tempYCoord > squareDimensions.getHeight()) {
            tempYCoord -= squareDimensions.getHeight();
            yCoord += 1;
        }

        if (xCoord > 7 || yCoord > 7) {
            resetClickEventVars();
            return;
        }

        clickSquareCoord = new int[] {xCoord, yCoord};

        if (Objects.isNull(clickEventClickedPiece)) {
            clickEventMovingFrom = clickSquareCoord;
            clickEventClickedPiece = chessboard.getPieceAtCoords(clickEventMovingFrom);

            if (Objects.isNull(clickEventClickedPiece)
                || (clickEventClickedPiece.pieceInt() & chessboard.getColorPlaying()) == 0) {
                resetClickEventVars();
                return;
            }
        } else {
            Chessboard.Move moveObj;
            clickEventMovingTo = clickSquareCoord;

            try {
                int[][] movesCoords = chessboard.getValidMoveCoordsArray(clickEventMovingFrom);
                if (!BoardArrays.arrayOfCoordsContainsCoord(movesCoords, clickEventMovingTo)) {
                    resetClickEventVars();
                    return;
                }

            } catch (AlgorithmBadArgumentException exception) {
                exception.printStackTrace();
                System.exit(1);
            }

            clickEventToCapturePiece = chessboard.getPieceAtCoords(clickEventMovingTo);

            boolean firstPieceIsKing = (clickEventClickedPiece.pieceInt() & Chessboard.KING) != 0;
            boolean secondPieceIsRook = Objects.nonNull(clickEventToCapturePiece)
                                            && (clickEventToCapturePiece.pieceInt() & Chessboard.ROOK) != 0;
            boolean bothPiecesAreSameColor = Objects.nonNull(clickEventToCapturePiece)
                                                 && (clickEventClickedPiece.pieceInt() & Chessboard.WHITE)
                                                 == (clickEventToCapturePiece.pieceInt() & Chessboard.WHITE);
            boolean moveIsCastling = firstPieceIsKing && secondPieceIsRook && bothPiecesAreSameColor;
            boolean moveIsCastlingKingside = moveIsCastling && xCoord == 0;
            boolean moveIsCastlingQueenside = moveIsCastling && xCoord == 7;

            /* The time that both pieces are the same color and it's a valid
               move is if the first one is a king and the second one is rook, bc
               that's how castling is signalled. */
            if (!moveIsCastling && Objects.nonNull(clickEventToCapturePiece)
                && (clickEventToCapturePiece.pieceInt() & chessboard.getColorPlaying()) != 0) {

                /* Both pieces are the same color (and it's not castling) so it's a no-op. */
                resetClickEventVars();
                return;

            } else if ((clickEventClickedPiece.pieceInt() & Chessboard.KING) != 0
                        && chessboard.isSquareThreatened(
                               clickEventMovingTo,
                               ((clickEventClickedPiece.pieceInt() & Chessboard.WHITE) != 0)
                                   ? Chessboard.BLACK : Chessboard.WHITE
                           )) {

                /* The piece is a King and moving it to that square would put it
                   in check, so movement is cancelled. */
                resetClickEventVars();
                return;
            }

            moveObj = new Chessboard.Move(clickEventClickedPiece, clickEventMovingFrom[0], clickEventMovingFrom[1],
                                          clickEventMovingTo[0], clickEventMovingTo[1],
                                          Objects.nonNull(clickEventToCapturePiece)
                                              ? clickEventToCapturePiece.pieceInt() : 0,
                                          moveIsCastlingKingside, moveIsCastlingQueenside, 0);

            try {
                chessboard.movePiece(moveObj);
            } catch (KingIsInCheckException exception) {
                resetClickEventVars();
                return;
            } catch (CastlingNotPossibleException exception) {
                JOptionPane.showMessageDialog(chessGameFrame, exception.getMessage());
                resetClickEventVars();
                return;
            }

            if ((moveObj.movingPiece().pieceInt() & Chessboard.PAWN) != 0) {
                int pieceColor = moveObj.movingPiece().pieceInt() ^ Chessboard.PAWN;
                int colorOnTop = chessboard.getColorOnTop();
                if ((pieceColor == colorOnTop) ? (moveObj.toYCoord() == 7) : (moveObj.toYCoord() == 0)) {
                    pawnHasntBeenPromotedYet = true;
                    pawnPrmtDBox = new PawnPromotionDBox(this, moveObj.toXCoord(), moveObj.toYCoord());
                }
            }

            resetClickEventVars();

            repaint();

            /* This timer is used both to introduce a hangtime between the
               player's move and the opposing move, and to break the opposing
               move logic out of this mouseClicked() method and run it in its
               own execution by using an event to trigger another, unconnected
               method call. */
            if (Objects.isNull(opposingMoveDelayTimer)) {
                opposingMoveDelayTimer = new Timer(timerDelayMlsec, this);
                opposingMoveDelayTimer.setActionCommand("move");
                opposingMoveDelayTimer.setRepeats(true);
            }
            opposingMoveDelayTimer.start();
        }
    }

    private void resetClickEventVars() {
        clickEventClickedPiece = null;
        clickEventMovingFrom = new int[] {-1, -1};
        clickEventToCapturePiece = null;
        clickEventMovingTo = new int[] {-1, -1};
    }

    public void actionPerformed(final ActionEvent event) {
        Chessboard.Move moveToMake;
        LocalDateTime timeRightNow;

        if (!event.getActionCommand().equals("move") || pawnHasntBeenPromotedYet) {
            return;
        }

        opposingMoveDelayTimer.stop();

        timeRightNow = LocalDateTime.now();
        System.out.println(dateTimeFormatter.format(timeRightNow) + " - starting algorithm");

        try {
            moveToMake = minimaxRunner.algorithmTopLevel();
        } catch (AlgorithmBadArgumentException | AlgorithmInternalException exception) {
            String exceptionClassName = exception.getClass().getName().split("^.*\\.")[1];
            JOptionPane.showMessageDialog(chessGameFrame, "Minimax algorithm experienced a " + exceptionClassName
                                                          + ":\n" + exception.getMessage());
            BoardArrays.printBoard(chessboard.getBoardArray());
            exception.printStackTrace();
            System.exit(1);
            return;
        } catch (KingIsInCheckmateException exception) {
            JOptionPane.showMessageDialog(chessGameFrame, exception.getMessage());
            System.exit(1);
            return;
        }

        try {
            chessboard.movePiece(moveToMake);
        } catch (KingIsInCheckException | CastlingNotPossibleException exception) {
            return;
        }

        timeRightNow = LocalDateTime.now();
        System.out.println(dateTimeFormatter.format(timeRightNow) + " - algorithm finished");

        repaint();
    }

    /**
     * An implementation of MouseListener.mouseEntered, required because I
     * implement that interface. I ignore the event, so 'assert true' is used as
     * a filler line.
     *
     * @param event The event to be processed.
     * @see java.awt.event.MouseListener
     */
    public void mouseEntered(final MouseEvent event) {
        assert true;
    }

    /**
     * An implementation of MouseListener.mouseExited, required because I
     * implement that interface. I ignore the event, so 'assert true' is used as
     * a filler line.
     *
     * @param event The event to be processed.
     * @see java.awt.event.MouseListener
     */
    public void mouseExited(final MouseEvent event) {
        assert true;
    }

    /**
     * An implementation of MouseListener.mousePressed, required because I
     * implement that interface. I ignore the event, so 'assert true' is used as
     * a filler line.
     *
     * @param event The event to be processed.
     * @see java.awt.event.MouseListener
     */
    public void mousePressed(final MouseEvent event) {
        assert true;
    }

    /**
     * An implementation of MouseListener.mouseReleased, required because I
     * implement that interface. I ignore the event, so 'assert true' is used as
     * a filler line.
     *
     * @param event The event to be processed.
     * @see java.awt.event.MouseListener
     */
    public void mouseReleased(final MouseEvent event) {
        assert true;
    }
}
