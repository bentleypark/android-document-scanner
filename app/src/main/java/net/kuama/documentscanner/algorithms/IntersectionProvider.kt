package net.kuama.documentscanner.algorithms

interface IntersectionProvider {
    fun intersection(segments: List<Segment>): List<Point>

    companion object {
        val DEFAULT: IntersectionProvider = BentleyOttmannIntersection
    }
}