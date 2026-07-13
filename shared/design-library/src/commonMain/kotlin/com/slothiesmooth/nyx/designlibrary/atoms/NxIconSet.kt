package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind

object NxIconSet {

    val Plus: ImageVector = NxVectorBuilder.stroke(
        "M12 5v14M5 12h14",
    )

    val ChevronLeft: ImageVector = NxVectorBuilder.stroke(
        "M15 18l-6-6 6-6",
    )

    val ChevronRight: ImageVector = NxVectorBuilder.stroke(
        "M9 18l6-6-6-6",
    )

    // eye outline + pupil circle (cx 12, cy 12, r 3) as arc path
    val Eye: ImageVector = NxVectorBuilder.stroke(
        "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z",
        "M15 12A3 3 0 1 1 9 12A3 3 0 0 1 15 12z",
    )

    // eye-off: two lid arcs + slashed pupil + diagonal strike line
    val EyeOff: ImageVector = NxVectorBuilder.stroke(
        "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94",
        "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19",
        "M14.12 14.12a3 3 0 1 1-4.24-4.24",
        "M1 1L23 23",
    )

    // body rect (x 3, y 11, w 18, h 11, rx 2) + closed shackle
    val Lock: ImageVector = NxVectorBuilder.stroke(
        "M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    )

    // same body rect + open shackle
    val Unlock: ImageVector = NxVectorBuilder.stroke(
        "M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11z",
        "M7 11V7a5 5 0 0 1 9.9-1",
    )

    // frame rect (x 3, y 3, w 18, h 18, rx 2) + sun dot (cx 8.5, cy 8.5, r 1.5) + mountain polyline
    val Image: ImageVector = NxVectorBuilder.stroke(
        "M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3z",
        "M10 8.5A1.5 1.5 0 1 1 7 8.5A1.5 1.5 0 0 1 10 8.5z",
        "M21 15L16 10L5 21",
    )

    // camera body + lens circle (cx 12, cy 13, r 4)
    val Camera: ImageVector = NxVectorBuilder.stroke(
        "M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z",
        "M16 13A4 4 0 1 1 8 13A4 4 0 0 1 16 13z",
    )

    // share-2: three nodes (r 3) + two connector lines
    val Share: ImageVector = NxVectorBuilder.stroke(
        "M21 5A3 3 0 1 1 15 5A3 3 0 0 1 21 5z",
        "M9 12A3 3 0 1 1 3 12A3 3 0 0 1 9 12z",
        "M21 19A3 3 0 1 1 15 19A3 3 0 0 1 21 19z",
        "M8.59 13.51L15.42 17.49",
        "M15.41 6.51L8.59 10.49",
    )

    // trash-2: lid line + can with handle + two content lines
    val Trash: ImageVector = NxVectorBuilder.stroke(
        "M3 6L5 6L21 6",
        "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
        "M10 11L10 17",
        "M14 11L14 17",
    )

    // archive box: lid rect (x 2, y 4, w 20, h 5, rx 2) + body + handle line
    val Archive: ImageVector = NxVectorBuilder.stroke(
        "M4 4H20A2 2 0 0 1 22 6V7A2 2 0 0 1 20 9H4A2 2 0 0 1 2 7V6A2 2 0 0 1 4 4z",
        "M4 9v9a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9",
        "M10 13L14 13",
    )

    // rotate-ccw: corner polyline + arc sweep (counterclockwise restore)
    val Restore: ImageVector = NxVectorBuilder.stroke(
        "M1 4L1 10L7 10",
        "M3.51 15a9 9 0 1 0 2.13-9.36L1 10",
    )

    // copy: front rect (x 9, y 9, w 13, h 13, rx 2) + back sheet
    val Copy: ImageVector = NxVectorBuilder.stroke(
        "M11 9H20A2 2 0 0 1 22 11V20A2 2 0 0 1 20 22H11A2 2 0 0 1 9 20V11A2 2 0 0 1 11 9z",
        "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1",
    )

    val Check: ImageVector = NxVectorBuilder.stroke(
        "M20 6L9 17l-5-5",
    )

    val Close: ImageVector = NxVectorBuilder.stroke(
        "M18 6L6 18M6 6l12 12",
    )

    // gear: hub circle (cx 12, cy 12, r 3) + tooth ring
    val Settings: ImageVector = NxVectorBuilder.stroke(
        "M15 12A3 3 0 1 1 9 12A3 3 0 0 1 15 12z",
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51" +
            "V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83" +
            "l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82" +
            "l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h0a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0" +
            "v.09a1.65 1.65 0 0 0 1 1.51h0a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06" +
            "a1.65 1.65 0 0 0-.33 1.82v0a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1" +
            "z",
    )

    // painter's palette: disk with thumb notch + four paint wells (r 1)
    val Palette: ImageVector = NxVectorBuilder.stroke(
        "M12 21a9 9 0 1 1 9-9c0 1.66-1.34 3-3 3h-2.5a2 2 0 0 0-2 2c0 .5.2 1 .5 1.4.3.4.5.8.5 1.1a1.5 1.5 0 0 1-1.5 1.5Z",
        "M7.5 11.5A1 1 0 1 1 5.5 11.5A1 1 0 0 1 7.5 11.5z",
        "M10 7.5A1 1 0 1 1 8 7.5A1 1 0 0 1 10 7.5z",
        "M14.5 6.5A1 1 0 1 1 12.5 6.5A1 1 0 0 1 14.5 6.5z",
        "M18.5 10.5A1 1 0 1 1 16.5 10.5A1 1 0 0 1 18.5 10.5z",
    )

    // info: ring (r 10) + stem + dot
    val Info: ImageVector = NxVectorBuilder.stroke(
        "M22 12A10 10 0 1 1 2 12A10 10 0 0 1 22 12z",
        "M12 16L12 12",
        "M12 8L12.01 8",
    )

    // alert-triangle: outline + exclamation stem + dot
    val Warning: ImageVector = NxVectorBuilder.stroke(
        "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z",
        "M12 9L12 13",
        "M12 17L12.01 17",
    )

    // safe: door rect (x 3, y 3, w 18, h 18, rx 2) + dial (r 4) + dial handle + two feet
    val Vault: ImageVector = NxVectorBuilder.stroke(
        "M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3z",
        "M16 12A4 4 0 1 1 8 12A4 4 0 0 1 16 12z",
        "M12 12L14.5 9.5",
        "M7 21L7 23",
        "M17 21L17 23",
    )
}

private val IconVectors: Map<NxIconKind, ImageVector> = mapOf(
    NxIconKind.Plus to NxIconSet.Plus,
    NxIconKind.ChevronLeft to NxIconSet.ChevronLeft,
    NxIconKind.ChevronRight to NxIconSet.ChevronRight,
    NxIconKind.Eye to NxIconSet.Eye,
    NxIconKind.EyeOff to NxIconSet.EyeOff,
    NxIconKind.Lock to NxIconSet.Lock,
    NxIconKind.Unlock to NxIconSet.Unlock,
    NxIconKind.Image to NxIconSet.Image,
    NxIconKind.Camera to NxIconSet.Camera,
    NxIconKind.Share to NxIconSet.Share,
    NxIconKind.Trash to NxIconSet.Trash,
    NxIconKind.Archive to NxIconSet.Archive,
    NxIconKind.Restore to NxIconSet.Restore,
    NxIconKind.Copy to NxIconSet.Copy,
    NxIconKind.Check to NxIconSet.Check,
    NxIconKind.Close to NxIconSet.Close,
    NxIconKind.Settings to NxIconSet.Settings,
    NxIconKind.Palette to NxIconSet.Palette,
    NxIconKind.Info to NxIconSet.Info,
    NxIconKind.Warning to NxIconSet.Warning,
    NxIconKind.Vault to NxIconSet.Vault,
)

private fun NxIconKind.vector(): ImageVector = IconVectors.getValue(this)

private val IconSizeDefault = 20.dp

@Composable
fun NxIcon(
    kind: NxIconKind,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    size: Dp = IconSizeDefault,
) {
    Icon(
        imageVector = kind.vector(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
