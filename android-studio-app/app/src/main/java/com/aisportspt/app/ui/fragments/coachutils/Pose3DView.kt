package com.aisportspt.app.ui.fragments.coachutils

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.max
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** 가벼운 3D 포즈 뷰어 (점/선 렌더링 + 터치 회전/줌) */
class Pose3DView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer3D = Pose3DRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer3D)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /** MediaPipe 3D 좌표(정규화) 입력: points = FloatArray(size = 3 * N), lines = (start,end) 인덱스 쌍 */
    fun updateSkeleton(points: FloatArray, lines: IntArray) {
        queueEvent { renderer3D.updateSkeleton(points, lines) }
    }

    private var lastX = 0f
    private var lastY = 0f
    private var lastDist = 0f

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.pointerCount) {
            1 -> {
                if (e.action == MotionEvent.ACTION_MOVE) {
                    val dx = e.x - lastX
                    val dy = e.y - lastY
                    queueEvent {
                        renderer3D.orbitAzimuth += dx * 0.5f
                        renderer3D.orbitElevation = (renderer3D.orbitElevation + dy * 0.5f)
                            .coerceIn(-89f, 89f)
                    }
                }
                lastX = e.x; lastY = e.y
            }
            2 -> {
                val dx = e.getX(0) - e.getX(1)
                val dy = e.getY(0) - e.getY(1)
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (lastDist != 0f) {
                    val scale = (dist / lastDist)
                    queueEvent {
                        renderer3D.distance /= max(0.5f, minOf(2f, scale))
                        renderer3D.distance = renderer3D.distance.coerceIn(0.4f, 6f)
                    }
                }
                lastDist = dist
            }
        }
        return true
    }

    private class Pose3DRenderer : Renderer {
        // 카메라 파라미터
        var orbitAzimuth = 0f       // 좌우 회전
        var orbitElevation = 15f    // 상하 회전
        var distance = 2.2f         // 줌(거리)

        private val proj = FloatArray(16)
        private val view = FloatArray(16)
        private val mvp = FloatArray(16)

        // 포즈 데이터
        private var pts = FloatArray(0)       // [x,y,z,...] in meters-ish
        private var lines = IntArray(0)       // [s0,e0,s1,e1,...]

        // 간단한 셰이더
        private val vShader = """
            attribute vec3 aPos;
            uniform mat4 uMVP;
            void main() { gl_Position = uMVP * vec4(aPos, 1.0); gl_PointSize = 8.0; }
        """.trimIndent()
        private val fShaderPts = """
            precision mediump float;
            void main(){ gl_FragColor = vec4(1.0,1.0,0.0,1.0); }
        """.trimIndent()
        private val fShaderLines = """
            precision mediump float;
            void main(){ gl_FragColor = vec4(0.1,0.6,1.0,1.0); }
        """.trimIndent()

        private var progPts = 0
        private var progLines = 0
        private var aPosPts = 0
        private var aPosLines = 0
        private var uMVPPts = 0
        private var uMVPLine = 0
        private var vbo = IntArray(1)

        private var vboPoints = IntArray(1)
        private var vboLines  = IntArray(1)
        private var lineVertexCount = 0

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            progPts = buildProgram(vShader, fShaderPts)
            progLines = buildProgram(vShader, fShaderLines)
            aPosPts = GLES20.glGetAttribLocation(progPts, "aPos")
            aPosLines = GLES20.glGetAttribLocation(progLines, "aPos")
            uMVPPts = GLES20.glGetUniformLocation(progPts, "uMVP")
            uMVPLine = GLES20.glGetUniformLocation(progLines, "uMVP")

            GLES20.glGenBuffers(1, vboPoints, 0)
            GLES20.glGenBuffers(1, vboLines, 0)
        }

        override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
            GLES20.glViewport(0, 0, w, h)
            val aspect = w.toFloat() / h
            Matrix.perspectiveM(proj, 0, 45f, aspect, 0.01f, 100f)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // 카메라(orbit)
            val eye = FloatArray(3)
            val az = Math.toRadians(orbitAzimuth.toDouble()).toFloat()
            val el = Math.toRadians(orbitElevation.toDouble()).toFloat()
            val r = distance
            eye[0] = (r * kotlin.math.cos(el) * kotlin.math.sin(az))
            eye[1] = (r * kotlin.math.sin(el))
            eye[2] = (r * kotlin.math.cos(el) * kotlin.math.cos(az))
            Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], 0f, 0f, 0f, 0f, 1f, 0f)
            Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)

            // 점 렌더
            if (pts.isNotEmpty()) {
                drawPoints()
                drawLines()
            }
        }

        fun updateSkeleton(points: FloatArray, linesIdx: IntArray) {
            if (points.isEmpty()) return

            val n = points.size / 3
            var cx = 0f; var cy = 0f; var cz = 0f
            for (i in 0 until n) {
                cx += points[3*i]; cy += points[3*i+1]; cz += points[3*i+2]
            }
            cx /= n; cy /= n; cz /= n

            // 1) 포인트 정규화/중심이동
            val scaled = FloatArray(points.size)
            for (i in 0 until n) {
                scaled[3*i  ] = (points[3*i]   - cx) * 1.2f
                scaled[3*i+1] = (points[3*i+1] - cy) * 1.2f
                scaled[3*i+2] = (points[3*i+2] - cz) * 1.2f
            }
            pts = scaled
            lines = linesIdx

            // 2) 점 VBO 업로드
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboPoints[0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                pts.size * 4,
                java.nio.ByteBuffer.allocateDirect(pts.size * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                    .asFloatBuffer().apply { put(pts); position(0) },
                GLES20.GL_DYNAMIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            // 3) "라인 전용" 버텍스 배열 생성: [s, e, s, e, ...] 순서로 복사
            val lineVerts = FloatArray(linesIdx.size * 3)
            var k = 0
            for (i in 0 until linesIdx.size step 2) {
                val s = linesIdx[i]
                val e = linesIdx[i + 1]
                // s
                lineVerts[k++] = pts[3*s]
                lineVerts[k++] = pts[3*s + 1]
                lineVerts[k++] = pts[3*s + 2]
                // e
                lineVerts[k++] = pts[3*e]
                lineVerts[k++] = pts[3*e + 1]
                lineVerts[k++] = pts[3*e + 2]
            }
            lineVertexCount = lineVerts.size / 3

            // 4) 라인 VBO 업로드
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboLines[0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                lineVerts.size * 4,
                java.nio.ByteBuffer.allocateDirect(lineVerts.size * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                    .asFloatBuffer().apply { put(lineVerts); position(0) },
                GLES20.GL_DYNAMIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }

        private fun drawPoints() {
            GLES20.glUseProgram(progPts)
            GLES20.glUniformMatrix4fv(uMVPPts, 1, false, mvp, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboPoints[0])
            GLES20.glEnableVertexAttribArray(aPosPts)
            GLES20.glVertexAttribPointer(aPosPts, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pts.size / 3)
            GLES20.glDisableVertexAttribArray(aPosPts)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }

        private fun drawLines() {
            if (lineVertexCount == 0) return
            GLES20.glUseProgram(progLines)
            GLES20.glUniformMatrix4fv(uMVPLine, 1, false, mvp, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboLines[0])
            GLES20.glEnableVertexAttribArray(aPosLines)
            GLES20.glVertexAttribPointer(aPosLines, 3, GLES20.GL_FLOAT, false, 0, 0)

            // 한 번에 모든 선분 그리기
            // (라인은 [s,e,s,e,...] 순서로 들어있으므로 GL_LINES로 OK)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertexCount)

            GLES20.glDisableVertexAttribArray(aPosLines)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            // (선 두께)
            // GLES20.glLineWidth(2f)  // ES2에서는 디바이스에 따라 무시될 수 있음
        }


        private fun buildProgram(vs: String, fs: String): Int {
            fun compile(type: Int, src: String): Int {
                val id = GLES20.glCreateShader(type)
                GLES20.glShaderSource(id, src)
                GLES20.glCompileShader(id)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] == 0) {
                    val log = GLES20.glGetShaderInfoLog(id)
                    GLES20.glDeleteShader(id)
                    throw RuntimeException("Shader compile error: $log")
                }
                return id
            }
            val vsId = compile(GLES20.GL_VERTEX_SHADER, vs)
            val fsId = compile(GLES20.GL_FRAGMENT_SHADER, fs)
            val prog = GLES20.glCreateProgram()
            GLES20.glAttachShader(prog, vsId)
            GLES20.glAttachShader(prog, fsId)
            GLES20.glLinkProgram(prog)
            val linked = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(prog)
                GLES20.glDeleteProgram(prog)
                throw RuntimeException("Program link error: $log")
            }
            return prog
        }
    }
}
