package cn.yueshutong.aspectj;

import cn.yueshutong.aspectj.annotations.Limiter;
import cn.yueshutong.propertry.LimiterRule;
import cn.yueshutong.rateLimiter.RateLimiter;
import cn.yueshutong.rateLimiter.RateLimiterFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
public class CurrentResourceAspect {
    private Map<String, RateLimiter> map = new ConcurrentHashMap<>();

    @Pointcut("@annotation(cn.yueshutong.aspectj.annotations.Limiter)")
    private void pointcut() {}

    @Around("pointcut() && @annotation(currentLimiter)")
    public Object around(ProceedingJoinPoint pjp, Limiter currentLimiter) throws Throwable {
        RateLimiter rateLimiter = initRateLimiter(pjp,currentLimiter);
        if (rateLimiter.tryAcquire()) {
            return pjp.proceed();
        }
        return null;
//        throw new CurrentLimitingException("Have current limiting");
    }

    /**
     * Initialize the current limiter
     */
    private RateLimiter initRateLimiter(ProceedingJoinPoint pjp, Limiter limiter) {
        String key = pjp.getSignature().toLongString();
        if (!map.containsKey(key)) {
            synchronized (this) {
                if (!map.containsKey(key)) {
                    map.put(key, RateLimiterFactory.of(getRateLimiter(key,limiter)));
                }
            }
        }
        return map.get(key);
    }

    private LimiterRule getRateLimiter(String key,Limiter limiter) {
        LimiterRule rule = new LimiterRule();
        if ("".equals(limiter.id())) {
            rule.setId(key);
        }else {
            rule.setId(limiter.id());
        }
        rule.setQps(limiter.QPS());
        rule.setAcquireModel(limiter.acquireModel());
        rule.setAlgorithm(limiter.algorithm());
        rule.setInitialDelay(limiter.initialDelay());
        rule.setCurrentModel(limiter.currentModel());
        return rule;
    }
}