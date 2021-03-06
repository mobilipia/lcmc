/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package rfb;

@SuppressWarnings({"unchecked", "deprecation", "serial"}) public class MsgTypes {

  // server to client

  public static final int framebufferUpdate = 0;
  public static final int setColourMapEntries = 1;
  public static final int bell = 2;
  public static final int serverCutText = 3;

  // client to server

  public static final int setPixelFormat = 0;
  public static final int fixColourMapEntries = 1;
  public static final int setEncodings = 2;
  public static final int framebufferUpdateRequest = 3;
  public static final int keyEvent = 4;
  public static final int pointerEvent = 5;
  public static final int clientCutText = 6;
}
