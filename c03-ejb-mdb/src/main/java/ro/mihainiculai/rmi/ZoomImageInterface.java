package ro.mihainiculai.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ZoomImageInterface extends Remote {
    byte[] zoomImage(byte[] bmpData, double zoomLevel) throws RemoteException;
}
