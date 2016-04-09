package com.painless.pc.util;

import android.graphics.Path;

public class ParserHelper {

  private final String mStr;
  private final int mLength;

  private int mPos = 0;

  private char mCmd = 'M';
  private float mVal = 0;

  private ParserHelper(String str) {
    mStr = str;
    mLength = mStr.length();
    skipSep();
  }

  public char getCmd() {
    char c = mStr.charAt(mPos);
    if (((c - 'A') * (c - 'Z') <= 0) || ((c - 'a') * (c - 'z') <= 0)) {
      mCmd = c;

      // char read.
      mPos ++;
      skipSep();
    }
    return mCmd;
  }

  private void skipSep() {
    while (mPos < mLength) {
      char c = mStr.charAt(mPos);
      if (c == ' ' || c == ',') {
        mPos++;
      } else {
        return;
      }
    }
  }

  public float nextFloat() {
    float dot = 0;
    boolean found = false;

    float negative = 1;
    float val = 0;

    loop:
      while (mPos < mLength) {
        char c = mStr.charAt(mPos);
        switch (c) {
          case '-': {
            if (found) {
              break loop;
            } else {
              negative *= -1;
              mPos++;
              break;
            }
          }
          case '+': {
            if (found) {
              break loop;
            } else {
              // No op
              mPos++;
              break;
            }
          }
          case '.': {
            if (dot != 0) {
              break loop;
            } else {
              dot = 0.1f;
              found = true;
              mPos++;
              break;
            }
          }
          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9': {
            found = true;
            if (dot != 0) {
              val = val + dot * (c - '0');
              dot = dot * 0.1f;
            } else {
              val = val * 10 + (c - '0');
            }
            mPos ++;
            break;
          }
          default : {
            break loop;
          }
        }
      }

    if (found) {
      mVal = negative * val;
      skipSep();
    }
    return mVal;
  }

  public static void parse(Path p, String s) {
    ParserHelper ph = new ParserHelper(s);
    float lastX = 0;
    float lastY = 0;
    float lastX1 = 0;
    float lastY1 = 0;
    float subPathStartX = 0;
    float subPathStartY = 0;
    char lastCmd = 'm';

    while (ph.mPos < ph.mLength) {
      char cmd = ph.getCmd();
      boolean wasCurve = false;
      switch (cmd) {
        case 'M':
        case 'm': {
          float x = ph.nextFloat();
          float y = ph.nextFloat();
          if (cmd == 'm') {
            subPathStartX += x;
            subPathStartY += y;
            p.rMoveTo(x, y);
            lastX += x;
            lastY += y;
          } else {
            subPathStartX = x;
            subPathStartY = y;
            p.moveTo(x, y);
            lastX = x;
            lastY = y;
          }
          break;
        }
        case 'Z':
        case 'z': {
          p.close();
          p.moveTo(subPathStartX, subPathStartY);
          lastX = subPathStartX;
          lastY = subPathStartY;
          lastX1 = subPathStartX;
          lastY1 = subPathStartY;
          wasCurve = true;
          break;
        }
        case 'L':
        case 'l': {
          float x = ph.nextFloat();
          float y = ph.nextFloat();
          if (cmd == 'l') {
            p.rLineTo(x, y);
            lastX += x;
            lastY += y;
          } else {
            p.lineTo(x, y);
            lastX = x;
            lastY = y;
          }
          break;
        }
        case 'H':
        case 'h': {
          float x = ph.nextFloat();
          if (cmd == 'h') {
            p.rLineTo(x, 0);
            lastX += x;
          } else {
            p.lineTo(x, lastY);
            lastX = x;
          }
          break;
        }
        case 'V':
        case 'v': {
          float y = ph.nextFloat();
          if (cmd == 'v') {
            p.rLineTo(0, y);
            lastY += y;
          } else {
            p.lineTo(lastX, y);
            lastY = y;
          }
          break;
        }
        case 'C':
        case 'c': {
          wasCurve = true;
          float x1 = ph.nextFloat();
          float y1 = ph.nextFloat();
          float x2 = ph.nextFloat();
          float y2 = ph.nextFloat();
          float x = ph.nextFloat();
          float y = ph.nextFloat();
          if (cmd == 'c') {
            x1 += lastX;
            x2 += lastX;
            x += lastX;
            y1 += lastY;
            y2 += lastY;
            y += lastY;
          }
          p.cubicTo(x1, y1, x2, y2, x, y);
          lastX1 = x2;
          lastY1 = y2;
          lastX = x;
          lastY = y;
          break;
        }
        case 'Q':
        case 'q': {
          wasCurve = true;
          float x1 = ph.nextFloat();
          float y1 = ph.nextFloat();
          float x = ph.nextFloat();
          float y = ph.nextFloat();
          if (cmd == 'q') {
            x1 += lastX;
            x += lastX;
            y1 += lastY;
            y += lastY;
          }
          p.quadTo(x1, y1, x, y);
          lastX1 = x1;
          lastY1 = y1;
          lastX = x;
          lastY = y;
          break;
        }
        case 'T':
        case 't': {
          // Draws a quadratic Bézier curve(reflective control point)
          wasCurve = true;
          float x = ph.nextFloat();
          float y = ph.nextFloat();
          if (cmd == 't') {
            x += lastX;
            y += lastY;
          }
          float x1 = lastX;
          float y1 = lastY;
          if (lastCmd == 'q' || lastCmd == 't' || lastCmd == 'Q' || lastCmd == 'T') {
          x1 = 2 * lastX - lastX1;
          y1 = 2 * lastY - lastY1;
          p.quadTo(x1, y1, x, y);
          lastX1 = x1;
          lastY1 = y1;
          lastX = x;
          lastY = y;
          break;
      }


        }
        case 'S':
        case 's': {
          wasCurve = true;
          float x2 = ph.nextFloat();
          float y2 = ph.nextFloat();
          float x = ph.nextFloat();
          float y = ph.nextFloat();
          if (cmd == 's') {
            x2 += lastX;
            x += lastX;
            y2 += lastY;
            y += lastY;
          }
          float x1 = 2 * lastX - lastX1;
          float y1 = 2 * lastY - lastY1;
          p.cubicTo(x1, y1, x2, y2, x, y);
          lastX1 = x2;
          lastY1 = y2;
          lastX = x;
          lastY = y;
          break;
        }
        case 'A':
        case 'a': {
          throw new RuntimeException("Arc is not yet supported");
        }
      }
      if (!wasCurve) {
        lastX1 = lastX;
        lastY1 = lastY;
      }
      lastCmd = cmd;
    }
  }
}
