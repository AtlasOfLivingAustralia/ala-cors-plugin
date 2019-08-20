package au.org.ala.web

class TestUtil {
    static def compareLists(a, b) {
        if (a == null || b == null) {
            return a == b
        } else if (a.size == b.size) {
            for (int i = 0; i < a.size; i++) {
                if (a[i].toString() != b[i].toString()) {
                    return false
                }
            }
            return true
        }
        return false
    }
}
