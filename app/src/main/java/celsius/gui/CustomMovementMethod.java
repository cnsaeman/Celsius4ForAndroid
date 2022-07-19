package celsius.gui;

import static android.text.Selection.*;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import com.atlantis.celsiusfa.MainActivity;

import atlantis.tools.Parser;
import celsius.Resources;

public class CustomMovementMethod extends LinkMovementMethod {

    private final Resources RSC;
    private final MainActivity mainActivity;

    public CustomMovementMethod(Resources RSC, MainActivity mainActivity) {
        this.RSC=RSC;
        this.mainActivity=mainActivity;
    }

    // Most of this code copied from LinkMovementMethod
    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer,
                                MotionEvent event) {
        int action = event.getAction();

        RSC.out("Touch Event");

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            RSC.out("Inner Touch Event");
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

            if (link.length != 0) {
                String linkURL=((URLSpan)link[0]).getURL();
                RSC.out("Link found: "+linkURL);
                if (linkURL.startsWith("http://$$")) {
                    if (action == MotionEvent.ACTION_UP) {
                        mainActivity.viewAttachment(Integer.valueOf(Parser.cutFrom(linkURL, "view-attachment-")));
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    RSC.out("Action_UP");
                    link[0].onClick(widget);
                } else if (action == MotionEvent.ACTION_DOWN) {
                    setSelection(buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]));
                }
                return true;
            } else {
                RSC.out("OTHER");
                removeSelection(buffer);
                // NEW CODE - call method B
            }
        }

        return Touch.onTouchEvent(widget, buffer, event);
    }

}