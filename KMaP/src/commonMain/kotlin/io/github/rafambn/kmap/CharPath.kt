package io.github.rafambn.kmap

import androidx.compose.ui.graphics.Path

class CharPath {
    val paths = mutableMapOf<Char, Path>()

    init {
        paths['0'] = Path().apply {
            moveTo(6.203125f, 0.203125f)
            cubicTo(4.378906f, 0.203125f, 2.972656f, -0.3125f, 1.984375f, -1.34375f)
            cubicTo(0.992188f, -2.375f, 0.5f, -3.84375f, 0.5f, -5.75f)
            lineTo(0.5f, -15.515625f)
            cubicTo(0.5f, -17.554688f, 0.976562f, -19.113281f, 1.9375f, -20.1875f)
            cubicTo(2.894531f, -21.257812f, 4.316406f, -21.796875f, 6.203125f, -21.796875f)
            cubicTo(8.117188f, -21.796875f, 9.550781f, -21.257812f, 10.5f, -20.1875f)
            cubicTo(11.457031f, -19.113281f, 11.9375f, -17.554688f, 11.9375f, -15.515625f)
            lineTo(11.9375f, -5.75f)
            cubicTo(11.9375f, -3.820312f, 11.441406f, -2.347656f, 10.453125f, -1.328125f)
            cubicTo(9.460938f, -0.304688f, 8.046875f, 0.203125f, 6.203125f, 0.203125f)
            moveTo(6.203125f, -3.765625f)
            cubicTo(6.523438f, -3.765625f, 6.785156f, -3.910156f, 6.984375f, -4.203125f)
            cubicTo(7.191406f, -4.503906f, 7.296875f, -4.851562f, 7.296875f, -5.25f)
            lineTo(7.296875f, -15.90625f)
            cubicTo(7.296875f, -16.53125f, 7.222656f, -17.003906f, 7.078125f, -17.328125f)
            cubicTo(6.929688f, -17.660156f, 6.640625f, -17.828125f, 6.203125f, -17.828125f)
            cubicTo(5.773438f, -17.828125f, 5.492188f, -17.660156f, 5.359375f, -17.328125f)
            cubicTo(5.222656f, -17.003906f, 5.15625f, -16.53125f, 5.15625f, -15.90625f)
            lineTo(5.15625f, -5.25f)
            cubicTo(5.15625f, -4.851562f, 5.25f, -4.503906f, 5.4375f, -4.203125f)
            cubicTo(5.632812f, -3.910156f, 5.890625f, -3.765625f, 6.203125f, -3.765625f)
            close()
        }
        paths['1'] = Path().apply {
            moveTo(2.96875f, -16.640625f)
            cubicTo(2.6875f, -16.285156f, 2.285156f, -16f, 1.765625f, -15.78125f)
            cubicTo(1.253906f, -15.570312f, 0.765625f, -15.46875f, 0.296875f, -15.46875f)
            lineTo(0.296875f, -18.90625f)
            cubicTo(1.035156f, -19.019531f, 1.769531f, -19.3125f, 2.5f, -19.78125f)
            cubicTo(3.226562f, -20.25f, 3.753906f, -20.863281f, 4.078125f, -21.625f)
            lineTo(7.546875f, -21.625f)
            lineTo(7.546875f, 0f)
            lineTo(2.96875f, 0f)
            close()
        }
        paths['2'] = Path().apply {
            moveTo(0.75f, -1.03125f)
            cubicTo(0.75f, -2.019531f, 0.90625f, -2.929688f, 1.21875f, -3.765625f)
            cubicTo(1.53125f, -4.609375f, 1.910156f, -5.363281f, 2.359375f, -6.03125f)
            cubicTo(2.816406f, -6.707031f, 3.410156f, -7.492188f, 4.140625f, -8.390625f)
            cubicTo(4.898438f, -9.335938f, 5.5f, -10.132812f, 5.9375f, -10.78125f)
            cubicTo(6.382812f, -11.4375f, 6.765625f, -12.1875f, 7.078125f, -13.03125f)
            cubicTo(7.390625f, -13.882812f, 7.546875f, -14.800781f, 7.546875f, -15.78125f)
            cubicTo(7.546875f, -16.375f, 7.460938f, -16.832031f, 7.296875f, -17.15625f)
            cubicTo(7.128906f, -17.488281f, 6.828125f, -17.65625f, 6.390625f, -17.65625f)
            cubicTo(5.660156f, -17.65625f, 5.296875f, -17.007812f, 5.296875f, -15.71875f)
            lineTo(5.296875f, -13.078125f)
            lineTo(0.75f, -13.078125f)
            lineTo(0.703125f, -14.40625f)
            cubicTo(0.703125f, -16.050781f, 0.867188f, -17.398438f, 1.203125f, -18.453125f)
            cubicTo(1.535156f, -19.503906f, 2.117188f, -20.304688f, 2.953125f, -20.859375f)
            cubicTo(3.796875f, -21.421875f, 4.957031f, -21.703125f, 6.4375f, -21.703125f)
            cubicTo(8.25f, -21.703125f, 9.648438f, -21.191406f, 10.640625f, -20.171875f)
            cubicTo(11.628906f, -19.160156f, 12.125f, -17.722656f, 12.125f, -15.859375f)
            cubicTo(12.125f, -14.671875f, 11.957031f, -13.59375f, 11.625f, -12.625f)
            cubicTo(11.300781f, -11.664062f, 10.898438f, -10.828125f, 10.421875f, -10.109375f)
            cubicTo(9.941406f, -9.390625f, 9.304688f, -8.539062f, 8.515625f, -7.5625f)
            cubicTo(7.953125f, -6.875f, 7.476562f, -6.257812f, 7.09375f, -5.71875f)
            cubicTo(6.71875f, -5.1875f, 6.378906f, -4.640625f, 6.078125f, -4.078125f)
            lineTo(11.96875f, -4.078125f)
            lineTo(11.96875f, 0f)
            lineTo(0.75f, 0f)
            close()
        }
        paths['3'] = Path().apply {
            moveTo(5.9375f, 0.25f)
            cubicTo(4.039062f, 0.25f, 2.65625f, -0.242188f, 1.78125f, -1.234375f)
            cubicTo(0.914062f, -2.234375f, 0.484375f, -3.757812f, 0.484375f, -5.8125f)
            lineTo(0.484375f, -8.0625f)
            lineTo(4.90625f, -8.0625f)
            lineTo(4.90625f, -5.8125f)
            cubicTo(4.90625f, -5.15625f, 4.988281f, -4.644531f, 5.15625f, -4.28125f)
            cubicTo(5.320312f, -3.925781f, 5.65625f, -3.75f, 6.15625f, -3.75f)
            cubicTo(6.507812f, -3.75f, 6.765625f, -3.875f, 6.921875f, -4.125f)
            cubicTo(7.085938f, -4.375f, 7.1875f, -4.671875f, 7.21875f, -5.015625f)
            cubicTo(7.25f, -5.359375f, 7.265625f, -5.859375f, 7.265625f, -6.515625f)
            lineTo(7.265625f, -7.046875f)
            cubicTo(7.265625f, -8.898438f, 6.65625f, -9.828125f, 5.4375f, -9.828125f)
            cubicTo(5.21875f, -9.828125f, 5.070312f, -9.820312f, 5f, -9.8125f)
            lineTo(5f, -13.671875f)
            cubicTo(5.769531f, -13.671875f, 6.347656f, -13.84375f, 6.734375f, -14.1875f)
            cubicTo(7.117188f, -14.53125f, 7.3125f, -15.082031f, 7.3125f, -15.84375f)
            cubicTo(7.3125f, -17.0625f, 6.960938f, -17.671875f, 6.265625f, -17.671875f)
            cubicTo(5.828125f, -17.671875f, 5.546875f, -17.5625f, 5.421875f, -17.34375f)
            cubicTo(5.296875f, -17.125f, 5.210938f, -16.847656f, 5.171875f, -16.515625f)
            cubicTo(5.128906f, -16.179688f, 5.097656f, -15.9375f, 5.078125f, -15.78125f)
            lineTo(5.078125f, -15.140625f)
            lineTo(0.609375f, -15.140625f)
            lineTo(0.578125f, -15.890625f)
            cubicTo(0.597656f, -17.867188f, 1.066406f, -19.320312f, 1.984375f, -20.25f)
            cubicTo(2.910156f, -21.175781f, 4.328125f, -21.640625f, 6.234375f, -21.640625f)
            cubicTo(9.898438f, -21.640625f, 11.734375f, -19.722656f, 11.734375f, -15.890625f)
            cubicTo(11.734375f, -14.765625f, 11.582031f, -13.867188f, 11.28125f, -13.203125f)
            cubicTo(10.976562f, -12.546875f, 10.460938f, -12.082031f, 9.734375f, -11.8125f)
            cubicTo(10.335938f, -11.507812f, 10.785156f, -11.132812f, 11.078125f, -10.6875f)
            cubicTo(11.367188f, -10.25f, 11.5625f, -9.710938f, 11.65625f, -9.078125f)
            cubicTo(11.75f, -8.441406f, 11.796875f, -7.585938f, 11.796875f, -6.515625f)
            cubicTo(11.796875f, -4.285156f, 11.328125f, -2.597656f, 10.390625f, -1.453125f)
            cubicTo(9.460938f, -0.316406f, 7.976562f, 0.25f, 5.9375f, 0.25f)
            close()
        }
        paths['4'] = Path().apply {
            moveTo(6.640625f, 0f)
            lineTo(6.640625f, -3.421875f)
            lineTo(0.21875f, -3.421875f)
            lineTo(0.21875f, -6.78125f)
            lineTo(4.296875f, -21.625f)
            lineTo(11.03125f, -21.625f)
            lineTo(11.03125f, -7.015625f)
            lineTo(12.1875f, -7.015625f)
            lineTo(12.1875f, -3.421875f)
            lineTo(11.03125f, -3.421875f)
            lineTo(11.03125f, 0f)
            moveTo(4.078125f, -7.015625f)
            lineTo(6.640625f, -7.015625f)
            lineTo(6.640625f, -17.90625f)
            close()
        }
        paths['5'] = Path().apply {
            moveTo(6.203125f, 0.296875f)
            cubicTo(4.484375f, 0.296875f, 3.109375f, -0.125f, 2.078125f, -0.96875f)
            cubicTo(1.054688f, -1.8125f, 0.546875f, -3.0625f, 0.546875f, -4.71875f)
            lineTo(0.546875f, -7.921875f)
            lineTo(5.046875f, -7.921875f)
            lineTo(5.046875f, -6.078125f)
            cubicTo(5.046875f, -5.347656f, 5.117188f, -4.769531f, 5.265625f, -4.34375f)
            cubicTo(5.421875f, -3.925781f, 5.734375f, -3.71875f, 6.203125f, -3.71875f)
            cubicTo(6.640625f, -3.71875f, 6.9375f, -3.859375f, 7.09375f, -4.140625f)
            cubicTo(7.257812f, -4.429688f, 7.34375f, -4.851562f, 7.34375f, -5.40625f)
            lineTo(7.34375f, -9.65625f)
            cubicTo(7.34375f, -10.34375f, 7.269531f, -10.890625f, 7.125f, -11.296875f)
            cubicTo(6.976562f, -11.710938f, 6.679688f, -11.921875f, 6.234375f, -11.921875f)
            cubicTo(5.410156f, -11.921875f, 5f, -11.320312f, 5f, -10.125f)
            lineTo(1.0625f, -10.125f)
            lineTo(1.0625f, -21.625f)
            lineTo(11.34375f, -21.625f)
            lineTo(11.34375f, -17.578125f)
            lineTo(5.15625f, -17.578125f)
            lineTo(5.15625f, -14.53125f)
            cubicTo(5.375f, -14.8125f, 5.664062f, -15.046875f, 6.03125f, -15.234375f)
            cubicTo(6.40625f, -15.421875f, 6.828125f, -15.515625f, 7.296875f, -15.515625f)
            cubicTo(9.097656f, -15.515625f, 10.328125f, -14.875f, 10.984375f, -13.59375f)
            cubicTo(11.648438f, -12.320312f, 11.984375f, -10.523438f, 11.984375f, -8.203125f)
            cubicTo(11.984375f, -6.265625f, 11.84375f, -4.695312f, 11.5625f, -3.5f)
            cubicTo(11.289062f, -2.300781f, 10.734375f, -1.367188f, 9.890625f, -0.703125f)
            cubicTo(9.054688f, -0.0351562f, 7.828125f, 0.296875f, 6.203125f, 0.296875f)
            close()
        }
        paths['6'] = Path().apply {
            moveTo(6.484375f, 0.203125f)
            cubicTo(5.015625f, 0.203125f, 3.847656f, -0.046875f, 2.984375f, -0.546875f)
            cubicTo(2.128906f, -1.054688f, 1.523438f, -1.8125f, 1.171875f, -2.8125f)
            cubicTo(0.828125f, -3.820312f, 0.65625f, -5.128906f, 0.65625f, -6.734375f)
            lineTo(0.65625f, -12.5625f)
            cubicTo(0.65625f, -14.789062f, 0.789062f, -16.546875f, 1.0625f, -17.828125f)
            cubicTo(1.34375f, -19.117188f, 1.910156f, -20.101562f, 2.765625f, -20.78125f)
            cubicTo(3.617188f, -21.457031f, 4.898438f, -21.796875f, 6.609375f, -21.796875f)
            cubicTo(7.546875f, -21.796875f, 8.410156f, -21.632812f, 9.203125f, -21.3125f)
            cubicTo(9.992188f, -21f, 10.625f, -20.523438f, 11.09375f, -19.890625f)
            cubicTo(11.5625f, -19.253906f, 11.796875f, -18.484375f, 11.796875f, -17.578125f)
            lineTo(11.796875f, -15.234375f)
            lineTo(7.625f, -15.234375f)
            lineTo(7.625f, -15.6875f)
            cubicTo(7.625f, -16.425781f, 7.554688f, -16.992188f, 7.421875f, -17.390625f)
            cubicTo(7.296875f, -17.796875f, 7f, -18f, 6.53125f, -18f)
            cubicTo(6.0625f, -18f, 5.726562f, -17.875f, 5.53125f, -17.625f)
            cubicTo(5.34375f, -17.375f, 5.25f, -16.988281f, 5.25f, -16.46875f)
            lineTo(5.25f, -12.125f)
            cubicTo(5.457031f, -12.519531f, 5.769531f, -12.832031f, 6.1875f, -13.0625f)
            cubicTo(6.601562f, -13.289062f, 7.097656f, -13.40625f, 7.671875f, -13.40625f)
            cubicTo(8.835938f, -13.40625f, 9.742188f, -13.164062f, 10.390625f, -12.6875f)
            cubicTo(11.035156f, -12.21875f, 11.484375f, -11.523438f, 11.734375f, -10.609375f)
            cubicTo(11.992188f, -9.691406f, 12.125f, -8.484375f, 12.125f, -6.984375f)
            cubicTo(12.125f, -4.691406f, 11.6875f, -2.921875f, 10.8125f, -1.671875f)
            cubicTo(9.9375f, -0.421875f, 8.492188f, 0.203125f, 6.484375f, 0.203125f)
            moveTo(6.328125f, -3.765625f)
            cubicTo(6.785156f, -3.765625f, 7.078125f, -3.945312f, 7.203125f, -4.3125f)
            cubicTo(7.328125f, -4.675781f, 7.390625f, -5.265625f, 7.390625f, -6.078125f)
            lineTo(7.390625f, -7.8125f)
            cubicTo(7.390625f, -8.988281f, 7.054688f, -9.578125f, 6.390625f, -9.578125f)
            cubicTo(5.628906f, -9.578125f, 5.25f, -9.066406f, 5.25f, -8.046875f)
            lineTo(5.25f, -5.375f)
            cubicTo(5.25f, -4.300781f, 5.609375f, -3.765625f, 6.328125f, -3.765625f)
            close()
        }
        paths['7'] = Path().apply {
            moveTo(19f, -23f)
            lineTo(19f, -21.5f)
            lineTo(10.763f, -3f)
            lineTo(8.574f, -3f)
            lineTo(16.587f, -21f)
            lineTo(6f, -21f)
            lineTo(6f, -23f)
            close()
        }
        paths['8'] = Path().apply {
            moveTo(6.203125f, 0.21875f)
            cubicTo(4.234375f, 0.21875f, 2.796875f, -0.335938f, 1.890625f, -1.453125f)
            cubicTo(0.992188f, -2.578125f, 0.546875f, -4.238281f, 0.546875f, -6.4375f)
            lineTo(0.546875f, -7.875f)
            cubicTo(0.546875f, -9.007812f, 0.679688f, -9.910156f, 0.953125f, -10.578125f)
            cubicTo(1.234375f, -11.253906f, 1.71875f, -11.757812f, 2.40625f, -12.09375f)
            cubicTo(1.757812f, -12.363281f, 1.289062f, -12.804688f, 1f, -13.421875f)
            cubicTo(0.71875f, -14.046875f, 0.578125f, -14.84375f, 0.578125f, -15.8125f)
            lineTo(0.578125f, -16.390625f)
            cubicTo(0.578125f, -18.265625f, 1.050781f, -19.644531f, 2f, -20.53125f)
            cubicTo(2.945312f, -21.425781f, 4.347656f, -21.875f, 6.203125f, -21.875f)
            cubicTo(8.097656f, -21.875f, 9.507812f, -21.421875f, 10.4375f, -20.515625f)
            cubicTo(11.375f, -19.609375f, 11.84375f, -18.175781f, 11.84375f, -16.21875f)
            cubicTo(11.84375f, -15.113281f, 11.707031f, -14.21875f, 11.4375f, -13.53125f)
            cubicTo(11.164062f, -12.851562f, 10.6875f, -12.363281f, 10f, -12.0625f)
            cubicTo(10.769531f, -11.695312f, 11.269531f, -11.125f, 11.5f, -10.34375f)
            cubicTo(11.738281f, -9.5625f, 11.859375f, -8.441406f, 11.859375f, -6.984375f)
            cubicTo(11.859375f, -6.703125f, 11.851562f, -6.519531f, 11.84375f, -6.4375f)
            cubicTo(11.84375f, -4.238281f, 11.394531f, -2.578125f, 10.5f, -1.453125f)
            cubicTo(9.613281f, -0.335938f, 8.179688f, 0.21875f, 6.203125f, 0.21875f)
            moveTo(6.203125f, -13.921875f)
            cubicTo(6.960938f, -13.921875f, 7.34375f, -14.609375f, 7.34375f, -15.984375f)
            cubicTo(7.34375f, -16.523438f, 7.253906f, -16.976562f, 7.078125f, -17.34375f)
            cubicTo(6.898438f, -17.71875f, 6.609375f, -17.90625f, 6.203125f, -17.90625f)
            cubicTo(5.804688f, -17.90625f, 5.519531f, -17.71875f, 5.34375f, -17.34375f)
            cubicTo(5.164062f, -16.976562f, 5.078125f, -16.523438f, 5.078125f, -15.984375f)
            cubicTo(5.078125f, -14.609375f, 5.453125f, -13.921875f, 6.203125f, -13.921875f)
            moveTo(6.203125f, -4.046875f)
            cubicTo(6.992188f, -4.046875f, 7.390625f, -4.757812f, 7.390625f, -6.1875f)
            lineTo(7.359375f, -7.265625f)
            cubicTo(7.359375f, -8.085938f, 7.289062f, -8.75f, 7.15625f, -9.25f)
            cubicTo(7.03125f, -9.75f, 6.710938f, -10f, 6.203125f, -10f)
            cubicTo(5.785156f, -10f, 5.484375f, -9.773438f, 5.296875f, -9.328125f)
            cubicTo(5.117188f, -8.890625f, 5.03125f, -8.34375f, 5.03125f, -7.6875f)
            cubicTo(5.03125f, -7.40625f, 5.035156f, -7.195312f, 5.046875f, -7.0625f)
            lineTo(5.046875f, -6.859375f)
            lineTo(5.03125f, -6.1875f)
            cubicTo(5.03125f, -4.757812f, 5.421875f, -4.046875f, 6.203125f, -4.046875f)
            close()
        }
        paths['9'] = Path().apply {
            moveTo(6.140625f, 0.203125f)
            cubicTo(5.191406f, 0.203125f, 4.328125f, 0.0390625f, 3.546875f, -0.28125f)
            cubicTo(2.773438f, -0.613281f, 2.15625f, -1.097656f, 1.6875f, -1.734375f)
            cubicTo(1.21875f, -2.367188f, 0.984375f, -3.140625f, 0.984375f, -4.046875f)
            lineTo(0.984375f, -6.359375f)
            lineTo(5.125f, -6.359375f)
            lineTo(5.125f, -5.9375f)
            cubicTo(5.125f, -5.21875f, 5.191406f, -4.644531f, 5.328125f, -4.21875f)
            cubicTo(5.460938f, -3.800781f, 5.753906f, -3.59375f, 6.203125f, -3.59375f)
            cubicTo(6.671875f, -3.59375f, 7f, -3.722656f, 7.1875f, -3.984375f)
            cubicTo(7.382812f, -4.242188f, 7.484375f, -4.632812f, 7.484375f, -5.15625f)
            lineTo(7.484375f, -9.484375f)
            cubicTo(7.285156f, -9.097656f, 6.976562f, -8.785156f, 6.5625f, -8.546875f)
            cubicTo(6.144531f, -8.316406f, 5.648438f, -8.203125f, 5.078125f, -8.203125f)
            cubicTo(3.898438f, -8.203125f, 2.988281f, -8.4375f, 2.34375f, -8.90625f)
            cubicTo(1.695312f, -9.375f, 1.25f, -10.066406f, 1f, -10.984375f)
            cubicTo(0.75f, -11.910156f, 0.625f, -13.125f, 0.625f, -14.625f)
            cubicTo(0.625f, -16.925781f, 1.0625f, -18.695312f, 1.9375f, -19.9375f)
            cubicTo(2.8125f, -21.175781f, 4.253906f, -21.796875f, 6.265625f, -21.796875f)
            cubicTo(7.734375f, -21.796875f, 8.894531f, -21.539062f, 9.75f, -21.03125f)
            cubicTo(10.601562f, -20.53125f, 11.207031f, -19.773438f, 11.5625f, -18.765625f)
            cubicTo(11.914062f, -17.765625f, 12.09375f, -16.460938f, 12.09375f, -14.859375f)
            lineTo(12.09375f, -9.03125f)
            cubicTo(12.09375f, -6.800781f, 11.957031f, -5.039062f, 11.6875f, -3.75f)
            cubicTo(11.414062f, -2.46875f, 10.851562f, -1.488281f, 10f, -0.8125f)
            cubicTo(9.144531f, -0.132812f, 7.859375f, 0.203125f, 6.140625f, 0.203125f)
            moveTo(6.359375f, -12.015625f)
            cubicTo(7.109375f, -12.015625f, 7.484375f, -12.523438f, 7.484375f, -13.546875f)
            lineTo(7.484375f, -16.234375f)
            cubicTo(7.484375f, -17.296875f, 7.125f, -17.828125f, 6.40625f, -17.828125f)
            cubicTo(5.957031f, -17.828125f, 5.671875f, -17.644531f, 5.546875f, -17.28125f)
            cubicTo(5.421875f, -16.925781f, 5.359375f, -16.34375f, 5.359375f, -15.53125f)
            lineTo(5.359375f, -13.78125f)
            cubicTo(5.359375f, -12.601562f, 5.691406f, -12.015625f, 6.359375f, -12.015625f)
            close()
        }
        paths['-'] = Path().apply {
            moveTo(0f, -15f)
            lineTo(15f, -15f)
            lineTo(15f, -5f)
            lineTo(0f, -5f)
            close()
        }
        paths[' '] = Path().apply {
            moveTo(0f, 0f)
            lineTo(10f, 10f)
            close()
        }
    }
}