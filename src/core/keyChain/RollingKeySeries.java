package core.keyChain;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import utilities.KeyCodeToChar;

/**
 * A chronologically ordered key series that have an upper limit of number of
 * key strokes and will remove the last one.
 */
public class RollingKeySeries extends KeySeries {

	private static final int DEFAULT_LIMIT = 100;

	private int limit;
	private LinkedList<KeyStroke> keys; // This is an alias for the underlying keys.

	public RollingKeySeries() {
		this(Arrays.asList(), DEFAULT_LIMIT);
	}

	public RollingKeySeries(List<KeyStroke> keys) {
		this(keys, DEFAULT_LIMIT);
	}

	public RollingKeySeries(List<KeyStroke> keys, int limit) {
		super();
		super.keys = new LinkedList<>();
		this.keys = (LinkedList<KeyStroke>) super.keys;

		this.limit = limit;

		for (KeyStroke key : keys) {
			addKeyStroke(key);
		}
	}

	/*
	 * Add all key strokes from another key chain.
	 */
	@Override
	public void addFrom(KeySeries other) {
		for (KeyStroke key : other.keys) {
			addKeyStroke(key);
		}
	}

	/**
	 * Add a single stroke to the key chain.
	 * @param stroke stroke to add.
	 */
	@Override
	public void addKeyStroke(KeyStroke stroke) {
		keys.add(stroke);
		if (keys.size() > limit) {
			keys.removeFirst();
		}
	}

	/**
	 * @return last key stroke in the series.
	 */
	public KeyStroke getLast() {
		if (keys.size() == 0) {
			return null;
		}
		return keys.getLast();
	}


	/**
	 * Get the string which would be typed out if all keys in this {@link KeySequence} are pressed in the specified order.
	 * Note that this includes effects of keys like SHIFT, CAPSLOCK, or NUMSLOCK.
	 * This means that this assumes both press and release activities are recorded with this instance.
	 */
	@Override
	public String getTypedString() {
		StringBuilder builder = new StringBuilder();
		KeyboardState keyboardState = KeyboardState.getDefault();

		for (KeyStroke keyStroke : getKeyStrokes()) {
			keyboardState = keyboardState.changeWith(keyStroke);
			if (keyStroke.isPressed()) {
				String s = KeyCodeToChar.getCharForCode(keyStroke.getKey(), keyboardState);
				builder.append(s);
			}
		}

		return builder.toString();
	}

	/**
	 * This method is only valid for input of class {@link KeySequence}.
	 * Checks whether the pressing key strokes in this object in the specified order
	 * will trigger the other {@link KeySequence}.
	 */
	@Override
	public boolean collideWith(KeySeries other) {
		if (other instanceof KeySequence) {
			return collideWithKeySequence((KeySequence) other);
		}

		if (other instanceof ActivationPhrase) {
			return collideWithActivationPhrase((ActivationPhrase) other);
		}

		throw new IllegalStateException("This method is not supported for this class: " + other.getClass());
	}

	private boolean collideWithKeySequence(KeySequence other) {
		List<KeyStroke> otherKeyStrokes = other.getKeyStrokes();
		if (otherKeyStrokes.size() > keys.size()) {
			return false;
		}

		ListIterator<KeyStroke> otherIterator = otherKeyStrokes.listIterator(otherKeyStrokes.size());
		ListIterator<KeyStroke> iterator = keys.listIterator(keys.size());
		for (int i = 0; i < otherKeyStrokes.size(); i++) {
			KeyStroke otherKeyStroke = otherIterator.previous();
			KeyStroke keyStroke = iterator.previous();

			if (!otherKeyStroke.equals(keyStroke)) {
				return false;
			}
		}
		return true;
	}

	private boolean collideWithActivationPhrase(ActivationPhrase other) {
		int lastCode = keys.listIterator(keys.size()).previous().getKey();
		boolean isTypedChar = KeyCodeToChar.hasCharForCode(lastCode, KeyboardState.getDefault());
		return getTypedString().endsWith(other.getValue()) && isTypedChar;
	}
}
