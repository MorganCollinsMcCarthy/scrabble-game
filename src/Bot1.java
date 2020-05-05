import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bot1 implements BotAPI {

	// The private API of Bot must not change
	// This is ONLY class that you can edit in the program
	// Rename Bot to the name of your team. Use camel case.
	// Bot may not alter the state of the game objects
	// It may only inspect the state of the board and the player objects

	private PlayerAPI me;
	private OpponentAPI opponent;
	private BoardAPI board;
	private UserInterfaceAPI info;
	private DictionaryAPI dictionary;
	private int turnCount;

	private String myLetter;
	private List<String> list;

	private static ArrayList<Word> realPlayableWords = new ArrayList<Word>();
	private static ArrayList<Integer> realPlayableWordsScore = new ArrayList<Integer>();

	private Square[][] boardCopy;
	private ArrayList<Word> wordsOnBoard = new ArrayList<Word>();

	Bot1(PlayerAPI me, OpponentAPI opponent, BoardAPI board, UserInterfaceAPI ui, DictionaryAPI dictionary) {
		this.me = me;
		this.opponent = opponent;
		this.board = board;
		this.info = ui;
		this.dictionary = dictionary;
		turnCount = 0;
	}

	public String getCommand() {return "PASS";}

	private String getMyLetter() {
		return myLetter;
	}

	private void setMyLetter(String myLetter) {
		this.myLetter = myLetter;
	}

	/** Method to score the real playable words */
	private void realPlayableWordscores() {
		int[] allLetterValues = { 1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 5, 1, 3, 1, 1, 3, 10, 1, 1, 1, 1, 4, 4, 8, 4, 10 };
		getBoardCopy();

		// loop through playable words
		for (int realWordIndex = 0; realWordIndex < realPlayableWords.size(); realWordIndex++) {
			int wordScore = 0, tripleWord = 0, doubleWord = 0, rowOffset = 0, colOffset = 0;
			int fullsubScore = 0;
			int row = realPlayableWords.get(realWordIndex).getRow();
			int col = realPlayableWords.get(realWordIndex).getFirstColumn();
			char letterOnBoard = '.'; // Initialize letterOnBoard

			if (!wordsOnBoard.isEmpty() && !board.isFirstPlay()) {

				if (boardCopy[row][col].isOccupied()) {
					letterOnBoard = boardCopy[row][col].getTile().getLetter();
				}

				if (realPlayableWords.get(realWordIndex).isVertical()) {
					if (boardCopy[row][col].isOccupied())
						rowOffset = realPlayableWords.get(realWordIndex).getLetters().indexOf(letterOnBoard);
					else
						rowOffset = 0;
				} else {
					if (boardCopy[row][col].isOccupied())
						colOffset = realPlayableWords.get(realWordIndex).getLetters().indexOf(letterOnBoard);
					else
						colOffset = 0;
				}
			}

			int letterRow = row - rowOffset;
			int letterCol = col - colOffset;

			for (int i = 0; i < realPlayableWords.get(realWordIndex).getLetters().length(); i++) {
				int letterValue = allLetterValues[realPlayableWords.get(realWordIndex).getLetter(i) - 'A'];
				if (boardCopy[letterRow][letterCol].isOccupied()) {
					wordScore += letterValue; // Increments score by letter value
				} else {
					// Increments score for double letters
					if (boardCopy[letterRow][letterCol].isDoubleLetter())
						wordScore += letterValue * 2;
					// Increments score for double letters
					else if (boardCopy[letterRow][letterCol].isTripleLetter())
						wordScore += letterValue * 2;
					// Increments score by letter value
					else
						wordScore += letterValue;
					// Increments doubleWord counter
					if (boardCopy[letterRow][letterCol].isDoubleWord())
						doubleWord++;
					// Increments tripleWord counter
					if (boardCopy[letterRow][letterCol].isTripleWord())
						tripleWord++;

					int subScore = 0;
					boolean hasSub = false;

					if ((realPlayableWords.get(realWordIndex).isHorizontal())
							&& !boardCopy[letterRow][letterCol].isOccupied()) {
						int subWordRow = letterRow;
						while (subWordRow - 1 >= 0 && boardCopy[subWordRow - 1][letterCol].isOccupied()) {
							subWordRow--;
							subScore += boardCopy[subWordRow][letterCol].getTile().getValue();
							hasSub = true;
						}
						subWordRow = letterRow;
						while (subWordRow + 1 <= 14 && boardCopy[subWordRow + 1][letterCol].isOccupied()) {
							subWordRow++;
							subScore += boardCopy[subWordRow][letterCol].getTile().getValue();
							hasSub = true;
						}
					}

					if (realPlayableWords.get(realWordIndex).isVertical()
							&& !boardCopy[letterRow][letterCol].isOccupied()) {
						int subWordCol = letterCol;
						while (subWordCol - 1 >= 0 && boardCopy[letterRow][subWordCol - 1].isOccupied()) {
							subWordCol--;
							subScore += boardCopy[letterRow][subWordCol].getTile().getValue();
							hasSub = true;
						}
						subWordCol = letterCol;
						// builds subword from occupied squares from the columns to the right
						while (subWordCol + 1 <= 14 && boardCopy[letterRow][subWordCol + 1].isOccupied()) {
							subWordCol++;
							subScore += boardCopy[letterRow][subWordCol].getTile().getValue();
							hasSub = true;
						}
					}
					if (hasSub) {
						if (boardCopy[letterRow][letterCol].isDoubleLetter())
							subScore += letterValue * 2;
						// Increments score for double letters
						else if (boardCopy[letterRow][letterCol].isTripleLetter())
							subScore += letterValue * 3;
						// Increments score by letter value
						else
							subScore += letterValue;
						// Increments doubleWord counter
						if (boardCopy[letterRow][letterCol].isDoubleWord())
							subScore *= 2;
						// Increments tripleWord counter
						if (boardCopy[letterRow][letterCol].isTripleWord())
							subScore *= 3;

					}
					fullsubScore += subScore;
				}

				if (realPlayableWords.get(realWordIndex).isVertical()) {
					letterRow++;
				} else
					letterCol++;

			}
			// Increments score for each doubleWord and each tripleWord
			if (doubleWord > 0)
				wordScore *= 2;
			if (tripleWord > 0)
				wordScore *= 3;
			int current = realPlayableWordsScore.get(realWordIndex);

			realPlayableWordsScore.set(realWordIndex, wordScore + fullsubScore + current);
		}
	}

	/** Boolean method to find real sub words */
	private boolean realSubWords(int startRow, int startCol, String word, boolean isHorizontal) {
		String subWord = null;

		boolean allReal = true;

		int subWordRow = startRow;
		int subWordCol = startCol;

		for (int i = 0; i < word.length(); i++) {
			subWord = "";

			if (isHorizontal) {
				subWord += word.charAt(i);
				// builds subword from occupied squares from the rows above
				while (subWordRow - 1 >= 0 && boardCopy[subWordRow - 1][subWordCol].isOccupied()) {
					subWordRow--;
					subWord = boardCopy[subWordRow][subWordCol].getTile().getLetter() + subWord;

				}
				subWordRow = startRow;
				// builds subword from occupied squares from the rows below
				while (subWordRow + 1 <= 14 && boardCopy[subWordRow + 1][subWordCol].isOccupied()) {
					subWordRow++;
					subWord = subWord + boardCopy[subWordRow][subWordCol].getTile().getLetter();
				}
				subWordCol++;
				subWordRow = startRow;
			}

			if (!isHorizontal) {
				subWord += word.charAt(i);
				// builds subword from occupied squares from the columns to the left
				while (subWordCol - 1 >= 0 && boardCopy[subWordRow][subWordCol - 1].isOccupied()) {
					subWordCol--;
					subWord = boardCopy[subWordRow][subWordCol].getTile().getLetter() + subWord;

				}
				subWordCol = startCol;
				// builds subword from occupied squares from the columns to the right
				while (subWordCol + 1 <= 14 && boardCopy[subWordRow][subWordCol + 1].isOccupied()) {
					subWordCol++;
					subWord = subWord + boardCopy[subWordRow][subWordCol].getTile().getLetter();
				}
				subWordRow++;
				subWordCol = startCol;
			}

			if (subWord.length() > 1) {
				// if list doesn't contain the subword return false
				if (!list.contains(subWord)) {
					allReal = false;
					return allReal;
				}
			}
		}
		// returns true for valid subwords
		return allReal;
	}

	/** Method to get an array list of the real playable words */
	private void findrealPlayableWords(String wordOnBoard, String myLetters, int row, int col, boolean horizontal) {
		int[] freq = toFreq(wordOnBoard + myLetters);
		for (String s : list) {
			int[] freqIn = toFreq(s);
			if (matches(freq, freqIn))
				if (wordOnBoard.equals("") || (s.contains(wordOnBoard) && !s.equals(wordOnBoard))) {
					if (!wordsOnBoard.isEmpty() && !board.isFirstPlay()) {
						getBoardCopy();
						int rowOffset = 0;
						int colOffset = 0;
						char letterOnBoard = '.'; // Initialize letterOnBoard
						boolean validPlay = true;
						if (boardCopy[row][col].isOccupied()) {
							letterOnBoard = boardCopy[row][col].getTile().getLetter();
						}

						if (horizontal) {
							if (!boardCopy[row][col].isOccupied())
								colOffset = 0;
							else
								colOffset = s.indexOf(letterOnBoard);

							if (col - colOffset >= 0 && col - colOffset + s.length() - 1 <= 14) {
								for (int i = col - colOffset, j = 0; i < col - colOffset + s.length(); i++, j++) {
									/*
									 * if the letter in the occupied squares do not match the letters of the
									 * playable word then the word is not playable
									 */
									if (boardCopy[row][i].isOccupied()
											&& s.charAt(j) != boardCopy[row][i].getTile().getLetter())
										validPlay = false;
								}
								if (validPlay && col - colOffset - 1 >= 0) {
									// if the played word is within the board boundaries but square is already
									// occupied
									if (boardCopy[row][col - colOffset - 1].isOccupied())
										validPlay = false;
								}
								if (validPlay && col - colOffset + s.length() - 1 + 1 <= 14) {
									// if the played word is within the board boundaries but square is already
									// occupied
									if (boardCopy[row][col - colOffset + s.length() - 1 + 1].isOccupied())
										validPlay = false;
								}

								// if returns true then the subword is added to realPlayableWords
								if (validPlay && realSubWords(row - rowOffset, col - colOffset, s, true)) {
									realPlayableWords.add(new Word(row, col, horizontal, s));
									if (s.length() - wordOnBoard.length() == 7)
										realPlayableWordsScore.add(50);
									else
										realPlayableWordsScore.add(0);
								}
							}
						}
						if (!horizontal) {
							if (!boardCopy[row][col].isOccupied())
								rowOffset = 0;
							else
								rowOffset = s.indexOf(letterOnBoard);

							if (row - rowOffset >= 0 && row - rowOffset + s.length() - 1 <= 14) {
								for (int i = row - rowOffset, j = 0; i < row - rowOffset + s.length(); i++, j++) {
									/*
									 * if the letter in the occupied squares do not match the letters of the
									 * playable word then the word is not playable
									 */
									if (boardCopy[i][col].isOccupied()
											&& s.charAt(j) != boardCopy[i][col].getTile().getLetter())
										validPlay = false;

								}
								if (validPlay && row - rowOffset - 1 >= 0) {
									// if the played word is within the board boundaries but square is already
									// occupied
									if (boardCopy[row - rowOffset - 1][col].isOccupied())
										validPlay = false;
								}

								if (row - rowOffset + s.length() - 1 + 1 <= 14) {
									// if the played word is within the board boundaries but square is already
									// occupied
									if (boardCopy[row - rowOffset + s.length() - 1 + 1][col].isOccupied())
										validPlay = false;
								}

								// if returns true then the subword is added to realPlayableWords
								if (validPlay && realSubWords(row - rowOffset, col - colOffset, s, false)) {
									realPlayableWords.add(new Word(row, col, horizontal, s));
									if (s.length() - wordOnBoard.length() == 7)
										realPlayableWordsScore.add(50);
									else
										realPlayableWordsScore.add(0);
								}
							}
						}
					} else {
						// else the word is playable and added to the array list
						realPlayableWords.add(new Word(row, col, horizontal, s));
						if (s.length() - wordOnBoard.length() == 7)
							realPlayableWordsScore.add(50);
						else
							realPlayableWordsScore.add(0);
					}
				}
		}
	}

	private boolean matches(int[] freq, int[] freqIn) {
		for (int i = 0; i < 26; i++) {
			if (freq[i] == 0 && freqIn[i] > 0)
				return false;
			else if (freq[i] < freqIn[i])
				return false;

		}
		return true;
	}

	private int[] toFreq(String string) {
		int[] freq = new int[26];
		for (char c : string.toCharArray()) {
			if ((c - 'A') >= 0 && (c - 'A') < 26)
				freq[c - 'A']++;
		}
		return freq;
	}

	/** Method to get a copy of the current state of the board */
	private void getBoardCopy() {
		boardCopy = new Square[15][15];
		for (int row = 0; row < 15; row++) {
			for (int col = 0; col < 15; col++) {
				boardCopy[row][col] = board.getSquareCopy(row, col);
			}
		}
	}

	/** Method to get an array list of the current horizontal words on the board */
	private void getHorizontalWords() {
		Word word; // Variable to store each word
		getBoardCopy();

		/*
		 * Iterate through each square of the board, checking to see if any squares are
		 * occupied
		 */
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				if (boardCopy[i][j].isOccupied()) {
					String wordString = boardCopy[i][j].getTile().toString();
					if (j == 14) // If it starts in the last column then the word only consists of one letter
					{
						word = new Word(i, j, true, wordString);
						wordsOnBoard.add(word);
						getPlayableUnoccupiedHorizontal(i, j); // Finds unoccupied squares which are above and below
																// letter
					} else if (j < 14) {
						if (!boardCopy[i][j + 1].isOccupied()) // If the square beside it isn't occupied then we have
																// found the whole word
						{
							word = new Word(i, j, true, wordString);
							wordsOnBoard.add(word);
							getPlayableUnoccupiedHorizontal(i, j); // Finds unoccupied squares which are above and below
																	// letter
						} else // Else we need to find all of the letters of the word
						{
							int col = j; // Stores column position of first letter of word
							getPlayableUnoccupiedHorizontal(i, j); // Finds unoccupied squares which are above and below
																	// letter

							j += 1;
							// Keep looping while there are occupied squares to the right of the occupied
							// square to find the whole word
							while (j < 15 && boardCopy[i][j].isOccupied()) {
								wordString += boardCopy[i][j].getTile().toString();
								getPlayableUnoccupiedHorizontal(i, j); // Finds unoccupied squares which are above and
																		// below letter
								j++;
							}

							word = new Word(i, col, true, wordString);
							wordsOnBoard.add(word);
						}
					}
				}
			}
		}
	}

	/** Method to get an array list of the current vertical words on the board */
	private void getVerticalWords() {
		Word word; // Variable to store each word
		getBoardCopy();

		/*
		 * Iterate through each square of the board, checking to see if any squares are
		 * occupied
		 */
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				if (boardCopy[j][i].isOccupied()) // If the square beside it isn't occupied then we have found the whole
													// word
				{
					String wordString = boardCopy[j][i].getTile().toString();
					if (j == 14) // If it starts in the last column then the word only consists of one letter
					{
						word = new Word(j, i, false, wordString);
						wordsOnBoard.add(word);
						getPlayableUnoccupiedVertical(j, i); // Finds unoccupied squares which are beside letter
					} else if (j < 14) {
						if (!boardCopy[j + 1][i].isOccupied()) {
							word = new Word(j, i, false, wordString);
							wordsOnBoard.add(word);
							getPlayableUnoccupiedVertical(j, i); // Finds unoccupied squares which are beside letter
						} else // Else we need to find all of the letters of the word
						{
							int row = j; // Stores row position of first letter of word
							getPlayableUnoccupiedVertical(j, i); // Finds unoccupied squares which are beside letter

							j += 1;
							// Keep looping while there are occupied squares below the occupied square to
							// find the whole word
							while (j < 15 && boardCopy[j][i].isOccupied()) {
								wordString += boardCopy[j][i].getTile().toString();
								getPlayableUnoccupiedVertical(j, i); // Finds unoccupied squares which are beside letter
								j++;
							}

							word = new Word(row, i, false, wordString);
							wordsOnBoard.add(word);
						}
					}
				}
			}
		}
	}

	/*
	 * Helper method to find the unoccupied squares above and below words on the
	 * board
	 */
	private void getPlayableUnoccupiedHorizontal(int row, int col) {
		Word word;

		/*
		 * If square above or below the square that's passed in is unoccupied then it is
		 * playable
		 */
		if (row < 14 && !boardCopy[row + 1][col].isOccupied()) {
			word = new Word(row + 1, col, true, "");
			wordsOnBoard.add(word);
		}
		if (row > 0 && !boardCopy[row - 1][col].isOccupied()) {
			word = new Word(row - 1, col, true, "");
			wordsOnBoard.add(word);
		}
	}

	/*
	 * Helper method to find the unoccupied squares above and below words on the
	 * board
	 */
	private void getPlayableUnoccupiedVertical(int row, int col) {
		Word word;

		/*
		 * If square to the left or right of the square that's passed in is unoccupied
		 * then it is playable
		 */
		if (col < 14 && !boardCopy[row][col + 1].isOccupied()) {
			word = new Word(row, col + 1, true, "");
			wordsOnBoard.add(word);
		}
		if (col > 0 && !boardCopy[row][col - 1].isOccupied()) {
			word = new Word(row, col - 1, true, "");
			wordsOnBoard.add(word);
		}
	}
}