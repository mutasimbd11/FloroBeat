package com.florosoft.florobeat.data.update

/**
 * Robust semantic version parser and comparator for FloroBeat updates.
 *
 * Implements standard Semantic Versioning (SemVer 2.0.0-style):
 * - Strips leading 'v' or 'V'
 * - Splits version numbers into numeric components (major.minor.patch[.build])
 * - Compares components numerically so "1.10.0" is newer than "1.9.0"
 * - Supports pre-release identifiers (e.g. "1.2.0-beta.1") where release outranks pre-release
 * - Provides strict and forgiving parsing
 */
class SemanticVersion(
    val major: Int,
    val minor: Int = 0,
    val patch: Int = 0,
    val build: Int = 0,
    val preRelease: String? = null,
    val raw: String = "",
) : Comparable<SemanticVersion> {

    /**
     * Returns true if this version is strictly greater than [other].
     */
    fun isNewerThan(other: SemanticVersion): Boolean = this > other

    /**
     * Returns true if this version is strictly greater than [versionString].
     */
    fun isNewerThan(versionString: String): Boolean = this > parse(versionString)

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        if (build != other.build) return build.compareTo(other.build)

        // Pre-release comparisons:
        // A version without a pre-release identifier has higher precedence than one with a pre-release.
        // E.g., 1.0.0 > 1.0.0-beta.1
        val p1 = preRelease
        val p2 = other.preRelease
        return when {
            p1 == null && p2 == null -> 0
            p1 == null && p2 != null -> 1
            p1 != null && p2 == null -> -1
            p1 != null && p2 != null -> comparePreRelease(p1, p2)
            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticVersion) return false
        return major == other.major &&
            minor == other.minor &&
            patch == other.patch &&
            build == other.build &&
            preRelease == other.preRelease
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + build
        result = 31 * result + (preRelease?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val base = "$major.$minor.$patch" + if (build > 0) ".$build" else ""
        return if (preRelease != null) "$base-$preRelease" else base
    }

    companion object {
        /**
         * Safely parses a version string (e.g. "v1.2.0", "1.6", "2.0.0-rc1").
         * Fallback to 0.0.0 if unparseable.
         */
        fun parse(version: String): SemanticVersion {
            val clean = version.trim().removePrefix("v").removePrefix("V")
            if (clean.isBlank()) return SemanticVersion(0, 0, 0, 0, null, version)

            val preSplit = clean.split('-', limit = 2)
            val numericPart = preSplit[0]
            val preRelease = preSplit.getOrNull(1)?.takeIf { it.isNotBlank() }

            val segments = numericPart.split('.').map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val major = segments.getOrElse(0) { 0 }
            val minor = segments.getOrElse(1) { 0 }
            val patch = segments.getOrElse(2) { 0 }
            val build = segments.getOrElse(3) { 0 }

            return SemanticVersion(
                major = major,
                minor = minor,
                patch = patch,
                build = build,
                preRelease = preRelease,
                raw = version,
            )
        }

        private fun comparePreRelease(pre1: String, pre2: String): Int {
            val parts1 = pre1.split('.')
            val parts2 = pre2.split('.')
            for (i in 0 until maxOf(parts1.size, parts2.size)) {
                val p1 = parts1.getOrNull(i)
                val p2 = parts2.getOrNull(i)
                if (p1 == null) return -1
                if (p2 == null) return 1
                val n1 = p1.toIntOrNull()
                val n2 = p2.toIntOrNull()
                val diff = when {
                    n1 != null && n2 != null -> n1.compareTo(n2)
                    n1 != null && n2 == null -> -1
                    n1 == null && n2 != null -> 1
                    else -> p1.compareTo(p2, ignoreCase = true)
                }
                if (diff != 0) return diff
            }
            return 0
        }
    }
}
