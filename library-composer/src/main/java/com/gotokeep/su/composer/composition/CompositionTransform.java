package com.gotokeep.su.composer.composition;

import android.animation.IntEvaluator;
import android.animation.TypeEvaluator;

import com.gotokeep.su.composer.animation.FloatEvaluator;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.time.TimeRange;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 13:34
 */
public class CompositionTransform {
    public static final int ALPHA = 0;
    public static final int TRANSLATE_X = 1;
    public static final int TRANSLATE_Y = 2;
    public static final int SCALE_X = 3;
    public static final int SCALE_Y = 4;
    public static final int ROTATION_X = 5;
    public static final int ROTATION_Y = 6;
    public static final int ROTATION_Z = 7;

    public static final int INDEX_TRANSLATE_X = 0;
    public static final int INDEX_TRANSLATE_Y = 1;
    public static final int INDEX_SCALE_X = 0;
    public static final int INDEX_SCALE_Y = 1;
    public static final int INDEX_ROTATION_X = 0;
    public static final int INDEX_ROTATION_Y = 1;
    public static final int INDEX_ROTATION_Z = 2;

    private static FloatEvaluator floatEvaluator = new FloatEvaluator();
    private static IntEvaluator intEvaluator = new IntEvaluator();

    private Instruction<Float> alphaInstruction;
    private Instruction<Integer> translateInstruction[] = new Instruction[2];
    private Instruction<Float> scaleInstruction[] = new Instruction[2];
    private Instruction<Float> rotationInstruction[] = new Instruction[3];

    public void setAlphaRampForTime(float alphaFrom, float alphaTo, TimeRange timeRange) {
        if (alphaInstruction == null) {
            alphaInstruction = new Instruction<>(alphaFrom, alphaTo, timeRange);
        } else {
            alphaInstruction.set(alphaFrom, alphaTo, timeRange);
        }
    }

    public void setTranslateRampForTime(int translateIndex, int translateFrom, int translateTo, TimeRange timeRange) {
        if (translateInstruction[translateIndex] == null) {
            translateInstruction[translateIndex] = new Instruction<>(translateFrom, translateTo, timeRange);
        } else {
            translateInstruction[translateIndex].set(translateFrom, translateTo, timeRange);
        }
    }

    public void setScaleInstruction(int scaleIndex, float scaleFrom, float scaleTo, TimeRange timeRange) {
        if (scaleInstruction[scaleIndex] == null) {
            scaleInstruction[scaleIndex] = new Instruction<>(scaleFrom, scaleTo, timeRange);
        } else {
            scaleInstruction[scaleIndex].set(scaleFrom, scaleTo, timeRange);
        }
    }

    public void setRotationInstruction(int rotationIndex, float rotationFrom, float rotationTo, TimeRange timeRange) {
        if (rotationInstruction[rotationIndex] == null) {
            rotationInstruction[rotationIndex] = new Instruction<>(rotationFrom, rotationTo, timeRange);
        } else {
            rotationInstruction[rotationIndex].set(rotationFrom, rotationTo, timeRange);
        }
    }

    public TimeRange getTimeRange(int instructionType) {
        Instruction instruction = null;
        switch (instructionType) {
            case ALPHA:
                instruction = alphaInstruction;
                break;
            case TRANSLATE_X:
            case TRANSLATE_Y:
                instruction = translateInstruction[instructionType - TRANSLATE_X];
                break;
            case SCALE_X:
            case SCALE_Y:
                instruction = scaleInstruction[instructionType - SCALE_X];
                break;
            case ROTATION_X:
            case ROTATION_Y:
            case ROTATION_Z:
                instruction = rotationInstruction[instructionType - ROTATION_X];
                break;
        }
        return instruction != null ? instruction.timeRange : null;
    }

    public Float getAlpha(Time time) {
        return alphaInstruction != null ? alphaInstruction.getValueAtTime(time, floatEvaluator) : null;
    }

    public Integer getTranslate(int translateIndex, Time time) {
        return translateInstruction[translateIndex] != null ?
                translateInstruction[translateIndex].getValueAtTime(time, intEvaluator) : null;
    }

    public Float getScale(int scaleIndex, Time time) {
        return scaleInstruction[scaleIndex] != null ?
                scaleInstruction[scaleIndex].getValueAtTime(time, floatEvaluator) : null;
    }

    public Float getRotation(int rotationIndex, Time time) {
        return rotationInstruction[rotationIndex] != null ?
                rotationInstruction[rotationIndex].getValueAtTime(time, floatEvaluator) : null;
    }

    private static final class Instruction<T> {
        T from;
        T to;
        TimeRange timeRange;

        public Instruction(T from, T to, TimeRange timeRange) {
            set(from, to, timeRange);
        }

        public void set(T from, T to, TimeRange timeRange) {
            this.from = from;
            this.to = to;
            this.timeRange = timeRange;
        }

        public T getValueAtTime(Time time, TypeEvaluator<T> evaluator) {
            if (Time.isInvalid(time)) {
                return null;
            } else {
                if (time.compareTo(timeRange.start) <= 0) {
                    return from;
                } else if (time.compareTo(timeRange.getRangeEnd()) >= 0) {
                    return to;
                } else {
                    return evaluator.evaluate((float) (time.value - timeRange.start.value) / timeRange.duration.value,
                            from, to);
                }
            }
        }
    }
}
