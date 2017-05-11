function long2str(a, b) {
    var c = a.length
        , d = c - 1 << 2;
    if (b) {
        var e = a[c - 1];
        if (d - 3 > e || e > d)
            return null;
        d = e
    }
    for (var f = 0; c > f; f++)
        a[f] = String.fromCharCode(255 & a[f], a[f] >>> 8 & 255, a[f] >>> 16 & 255, a[f] >>> 24 & 255);
    return b ? a.join("").substring(0, d) : a.join("")
}
function str2long(a, b) {
    for (var c = a.length, d = [], e = 0; c > e; e += 4)
        d[e >> 2] = a.charCodeAt(e) | a.charCodeAt(e + 1) << 8 | a.charCodeAt(e + 2) << 16 | a.charCodeAt(e + 3) << 24;
    return b && (d[d.length] = c),
        d
}
function xxtea_encrypt(a, b) {
    if ("" == a)
        return "";
    var c = str2long(a, !0)
        , d = str2long(b, !1);
    d.length < 4 && (d.length = 4);
    for (var e, f, g, h = c.length - 1, i = c[h], j = c[0], k = 2654435769, l = Math.floor(6 + 52 / (h + 1)), m = 0; 0 < l--; ) {
        for (m = m + k & 4294967295,
                 f = m >>> 2 & 3,
                 g = 0; h > g; g++)
            j = c[g + 1],
                e = (i >>> 5 ^ j << 2) + (j >>> 3 ^ i << 4) ^ (m ^ j) + (d[3 & g ^ f] ^ i),
                i = c[g] = c[g] + e & 4294967295;
        j = c[0],
            e = (i >>> 5 ^ j << 2) + (j >>> 3 ^ i << 4) ^ (m ^ j) + (d[3 & g ^ f] ^ i),
            i = c[h] = c[h] + e & 4294967295
    }
    return long2str(c, !1)
}
function xxtea_decrypt(a, b) {
    if ("" == a)
        return "";
    var c = str2long(a, !1)
        , d = str2long(b, !1);
    d.length < 4 && (d.length = 4);
    for (var e, f, g, h = c.length - 1, i = c[h - 1], j = c[0], k = 2654435769, l = Math.floor(6 + 52 / (h + 1)), m = l * k & 4294967295; 0 != m; ) {
        for (f = m >>> 2 & 3,
                 g = h; g > 0; g--)
            i = c[g - 1],
                e = (i >>> 5 ^ j << 2) + (j >>> 3 ^ i << 4) ^ (m ^ j) + (d[3 & g ^ f] ^ i),
                j = c[g] = c[g] - e & 4294967295;
        i = c[h],
            e = (i >>> 5 ^ j << 2) + (j >>> 3 ^ i << 4) ^ (m ^ j) + (d[3 & g ^ f] ^ i),
            j = c[0] = c[0] - e & 4294967295,
            m = m - k & 4294967295
    }
    return long2str(c, !0)
}
function ntos(a) {
    return a = a.toString(16),
    1 == a.length && (a = "0" + a),
        a = "%" + a,
        unescape(a)
}
function decodeHex(str) {
    str = str.replace(new RegExp("s/[^0-9a-zA-Z]//g"));
    for (var result = "", nextchar = "", i = 0; i < str.length; i++)
        nextchar += str.charAt(i),
        2 == nextchar.length && (result += ntos(eval("0x" + nextchar)),
            nextchar = "");
    return result
}
function decode(){
    var str = xxtea_decrypt(decodeHex(css), "u0076");
    buffer.append(str);

    out.println(str);
}

decode();