/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2020 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.utils;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.vision.text.Text;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.stage.StageActivity;

import java.util.ArrayList;
import java.util.List;

import static org.catrobat.catroid.common.Constants.COORDINATE_TRANSFORMATION_OFFSET;
import static org.catrobat.catroid.common.ScreenValues.SCREEN_HEIGHT;
import static org.catrobat.catroid.common.ScreenValues.SCREEN_WIDTH;

public final class TextBlockUtil {
	private static List<Text.TextBlock> textBlocks;
	private static List<String> textBlockLanguages;
	private static int imageWidth;
	private static int imageHeight;
	private static final int MAX_TEXT_SIZE = 100;
	private static LanguageIdentifier identifier;

	private TextBlockUtil() {
		// static class, nothing to do
	}

	public static void setTextBlocks(List<Text.TextBlock> text, int width, int height) {
		textBlocks = text;
		imageWidth = width;
		imageHeight = height;
		if (textBlockLanguages != null) {
			textBlockLanguages.clear();
		} else {
			textBlockLanguages = new ArrayList<>();
		}

		if (identifier == null) {
			identifier = LanguageIdentification.getClient();
		}

		for (int i = 0; i < text.size(); i++) {
			Text.TextBlock textBlock = text.get(i);
			int finalIndex = i;
			identifier.identifyLanguage(textBlock.getText()).addOnSuccessListener(langCode -> {
				if (finalIndex < textBlocks.size() - 1) {
					textBlockLanguages.add(finalIndex, langCode);
				} else {
					textBlockLanguages.add(langCode);
				}
			});
		}
	}

	public static String getTextBlock(int index) {
		if (textBlocks != null && (textBlocks.size() > (index - 1)) && index >= 1) {
			return textBlocks.get(index - 1).getText();
		}
		return "0";
	}

	public static String getTextBlockLanguage(int index) {
		if (textBlockLanguages != null && (textBlockLanguages.size() > (index - 1)) && index >= 1) {
			return textBlockLanguages.get(index - 1);
		}
		return "0";
	}

	public static Point getCenterCoordinates(int index) {
		if (textBlocks != null && (textBlocks.size() > (index - 1)) && index >= 1) {
			Rect textBlockBounds = textBlocks.get((index - 1)).getBoundingBox();
			try {
				boolean invertAxis = StageActivity.getActiveCameraManager().isCameraFacingFront();
				float aspectRatio = (float) imageWidth / imageHeight;

				if (ProjectManager.getInstance().isCurrentProjectLandscapeMode()) {
					float relativeY = -1 * textBlockBounds.exactCenterX() / imageWidth;
					relativeY = invertAxis ? relativeY : 1 - relativeY;
					return coordinatesFromRelativePosition(
							textBlockBounds.exactCenterY() / imageHeight,
							SCREEN_WIDTH / aspectRatio,
							relativeY,
							(float) SCREEN_WIDTH
					);
				} else {
					float relativeX = textBlockBounds.exactCenterX() / imageHeight;
					relativeX = invertAxis ? 1 - relativeX : relativeX;
					return coordinatesFromRelativePosition(
							relativeX,
							SCREEN_HEIGHT / aspectRatio,
							1 - textBlockBounds.exactCenterY() / imageWidth,
							(float) SCREEN_HEIGHT
					);
				}
			} catch (NullPointerException e) {
				Log.d(TextBlockUtil.class.getSimpleName(), "The device has no front camera.", e);
				return new Point(0, 0);
			}
		} else {
			return new Point(0, 0);
		}
	}

	private static Point coordinatesFromRelativePosition(float relativeX, float width,
			float relativeY, float height) {
		return new Point(
				(int) Math.round(width * (relativeX - COORDINATE_TRANSFORMATION_OFFSET)),
				(int) Math.round(height * (relativeY - COORDINATE_TRANSFORMATION_OFFSET))
		);
	}

	public static double getSize(int index) {
		if (textBlocks != null && (textBlocks.size() > (index - 1)) && index >= 1) {
			Rect textBlockBounds = textBlocks.get((index - 1)).getBoundingBox();
			float relativeTextBlockSize = ((float) textBlockBounds.width()) / imageWidth;
			if (relativeTextBlockSize > 1f) {
				relativeTextBlockSize = 1f;
			}
			return (int) (MAX_TEXT_SIZE * relativeTextBlockSize);
		} else {
			return 0f;
		}
	}
}
