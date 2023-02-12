package android.hardware.input;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.view.InputEvent;

public interface IInputManager extends IInterface {
    boolean injectInputEvent(InputEvent ev, int mode) throws RemoteException;

    abstract class Stub extends Binder implements IInputManager {
        public static IInputManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
