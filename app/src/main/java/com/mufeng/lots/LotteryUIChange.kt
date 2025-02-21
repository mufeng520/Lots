package com.mufeng.lots

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.lang.ref.WeakReference

// 抽签策略接口（核心）
interface LotteryStrategy {
    fun setupUI(container: ViewGroup)  // 初始化对应UI
    fun startAnimation(context: Context, result: String) // 启动动画
    fun clearUI(container: ViewGroup)  // 清理UI元素
}
// 纯文本策略
class TextOnlyStrategy : LotteryStrategy {
    // 使用弱引用避免内存泄漏
    private var tvResultWeakRef: WeakReference<TextView>? = null
    private var animator: Animator? = null

    override fun setupUI(container: ViewGroup) {
        // 每次重新创建视图
        val textView = TextView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT  //宽高自适应
            ).apply {
                gravity = Gravity.CENTER  //居中
            }
//            setTextColor(ContextCompat.getColor(context, R.color.result_text))//颜色
            textSize = 32f //字号
            typeface = ResourcesCompat.getFont(context,R.font.lottery_font)//字体
            visibility = View.INVISIBLE//初始隐藏视图
            tag = "TEXT_RESULT_${System.currentTimeMillis()}" // 动态tag避免复用问题
        }

        container.addView(textView)
        tvResultWeakRef = WeakReference(textView)
    }

    override fun startAnimation(context: Context, result: String) {
        tvResultWeakRef?.get()?.let { tv ->
            tv.text = result
            tv.visibility = View.VISIBLE

            // 使用属性动画资源
            animator = AnimatorInflater.loadAnimator(context, R.animator.text_fade_in).apply {
                setTarget(tv)
                addListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        addTextDecorations(tv)
                    }
                })
                start()
            }
        }
    }

    private fun addTextDecorations(textView: TextView) {
        // 使用动态创建的背景避免持有引用
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
//            setStroke(4, ContextCompat.getColor(textView.context, R.color.stroke_color))
        }

        textView.background = drawable
    }

    override fun clearUI(container: ViewGroup) {
        // 清理所有资源
        animator?.cancel()
        animator = null

        tvResultWeakRef?.get()?.let {
            container.removeView(it)
        }
        tvResultWeakRef = null
    }
}



// 签筒策略完整实现
class TubeStrategy : LotteryStrategy {
    // 使用弱引用持有视图
    private var ivTubeWeakRef: WeakReference<ImageView>? = null
    private var ivSignWeakRef: WeakReference<ImageView>? = null
    private var tvResultWeakRef: WeakReference<TextView>? = null
    private var currentAnimator: AnimatorSet? = null

    override fun setupUI(container: ViewGroup) {
        // 创建签筒视图（每次新建）
        val ivTube = ImageView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(120, context),
                dpToPx(160, context)  //宽高
            ).apply {
                gravity = Gravity.CENTER//居中
            }
//            setImageResource(R.drawable.tube)
//            visibility = View.INVISIBLE //初始隐藏视图
            tag = "TUBE_${System.currentTimeMillis()}"
        }

        // 创建签视图
        val ivSign = ImageView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(80, context),
                dpToPx(200, context)
            )
//            setImageResource(R.drawable.bamboo_sign)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.INVISIBLE
            tag = "SIGN_${System.currentTimeMillis()}"
        }

        // 创建结果文字
        val tvResult = TextView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
//            setTextColor(ContextCompat.getColor(context, R.color.result_text))
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            visibility = View.INVISIBLE
            tag = "TUBE_RESULT_${System.currentTimeMillis()}"
        }

        // 添加到容器
        container.addView(ivTube)
        container.addView(ivSign)
        container.addView(tvResult)

        // 保存弱引用
        ivTubeWeakRef = WeakReference(ivTube)
        ivSignWeakRef = WeakReference(ivSign)
        tvResultWeakRef = WeakReference(tvResult)

        // 初始位置设置
        postLayout {
            ivSignWeakRef?.get()?.apply {
                x = ivTube.x + ivTube.width / 4f
                y = ivTube.y + ivTube.height - dpToPx(20, context)
            }
        }
    }

    override fun startAnimation(context: Context, result: String) {
        currentAnimator?.cancel()

        val animatorSet = AnimatorSet()
        currentAnimator = animatorSet

        // 获取视图实例
        val ivTube = ivTubeWeakRef?.get() ?: return
        val ivSign = ivSignWeakRef?.get() ?: return
        val tvResult = tvResultWeakRef?.get() ?: return

        // 第一阶段：签筒入场
        val tubeEnter = ObjectAnimator.ofPropertyValuesHolder(
            ivTube,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1f)
        ).apply {
            duration = 300
            interpolator = OvershootInterpolator()
        }

        // 第二阶段：摇晃动画
        val shakeAnim = createShakeAnimation(ivTube)

        // 第三阶段：抽签动画
        val drawAnim = createDrawAnimation(context, ivSign, tvResult, result)

        animatorSet.playSequentially(tubeEnter, shakeAnim, drawAnim)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 清理动画引用
                currentAnimator = null
            }
        })
        animatorSet.start()
    }

    private fun createShakeAnimation(ivTube: ImageView): AnimatorSet {
        ivTube.visibility = View.VISIBLE
        return AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ivTube, "rotation", 0f, 15f, -12f, 8f, 0f)
                    .setDuration(600),
                ObjectAnimator.ofFloat(ivTube, "translationY", 0f, -20f, 0f)
                    .setDuration(600)
            )
            interpolator = OvershootInterpolator()
        }
    }

    private fun createDrawAnimation(
        context: Context,
        ivSign: ImageView,
        tvResult: TextView,
        result: String
    ): AnimatorSet {
        return AnimatorSet().apply {
            // 签的移动路径
            val path = Path().apply {
                lineTo(400f, 600f)
                lineTo(ivSign.x, ivSign.y)// - dpToPx(300, context))
            }

            // 动画组合
            val signMove = ObjectAnimator.ofFloat(ivSign, View.X, View.Y, path).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }

            val signRotate = ObjectAnimator.ofFloat(ivSign, View.ROTATION, -90f, 0f)
                .setDuration(600)

            val textAnim = ObjectAnimator.ofPropertyValuesHolder(
                tvResult,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1.2f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1.2f, 1f)
            ).apply {
                startDelay = 300
                duration = 500
            }

            playTogether(signMove, signRotate)
            play(textAnim).after(signMove)

            doOnStart {
                ivSign.visibility = View.VISIBLE
                tvResult.text = result
            }

            doOnEnd {
                tvResult.visibility = View.VISIBLE
                startBreathingEffect(tvResult)
            }
        }
    }

    private fun startBreathingEffect(tvResult: TextView) {
        ObjectAnimator.ofPropertyValuesHolder(
            tvResult,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.05f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.05f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 1f)
        ).apply {
            duration = 1200
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    override fun clearUI(container: ViewGroup) {
        currentAnimator?.cancel()
        currentAnimator = null

        // 移除所有视图
        ivTubeWeakRef?.get()?.let { container.removeView(it) }
        ivSignWeakRef?.get()?.let { container.removeView(it) }
        tvResultWeakRef?.get()?.let { container.removeView(it) }

        // 清理引用
        ivTubeWeakRef = null
        ivSignWeakRef = null
        tvResultWeakRef = null
    }

    // region 工具方法
    private fun dpToPx(dp: Int, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private inline fun postLayout(crossinline action: () -> Unit) {
        ivTubeWeakRef?.get()?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                ivTubeWeakRef?.get()?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                action()
            }
        })
    }
    // endregion
}

class RouletteStrategy : LotteryStrategy {
    override fun setupUI(container: ViewGroup) { /* 初始化转盘UI */ }
    override fun startAnimation(context: Context, result: String) { /* 转盘旋转动画 */ }
    override fun clearUI(container: ViewGroup) { /* 移除转盘相关视图 */ }
}

// 策略枚举
enum class LotteryType(val strategy: LotteryStrategy) {
    TUBE(TubeStrategy()),
    TEXT(TextOnlyStrategy()), // 新增文本策略
    ROULETTE(RouletteStrategy()),
//    DICE(DiceStrategy)
}


// 策略管理器
class LotteryManager(private var currentStrategy: LotteryStrategy) {
    fun safeChangeStrategy(strategy: LotteryStrategy, container: ViewGroup) {
        currentStrategy.clearUI(container)
        currentStrategy = strategy
        currentStrategy.setupUI(container)
    }
    fun changeStrategy(strategy: LotteryStrategy) {
        currentStrategy = strategy
    }

    fun executeStrategy(container: ViewGroup, context: Context, result: String) {
        currentStrategy.startAnimation(context, result)
    }
}