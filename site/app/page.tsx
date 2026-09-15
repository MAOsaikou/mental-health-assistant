"use client";

import { useState } from "react";

const moods = [
  { label: "很低落", symbol: "雨", reply: "今天已经很辛苦了。先把要求放低一点，只照顾好眼前十分钟。" },
  { label: "有点累", symbol: "雾", reply: "累不是退步。允许自己慢下来，也是一种认真生活。" },
  { label: "还可以", symbol: "云", reply: "平平常常也很好。留意一件让你稍微舒服的小事。" },
  { label: "挺轻松", symbol: "风", reply: "把这份轻松收好。它会提醒你，状态一直都在流动。" },
  { label: "很有力量", symbol: "光", reply: "真好。把一点力量留给明天，也分享给此刻的自己。" },
];

export default function Home() {
  const [selectedMood, setSelectedMood] = useState<number | null>(null);

  const scrollToCheckIn = () => {
    document.getElementById("check-in")?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top" aria-label="小光心理助手首页">
          <span className="brand-mark" aria-hidden="true">光</span>
          <span>小光心理助手</span>
        </a>
        <nav aria-label="主导航">
          <a href="#check-in">心情签到</a>
          <a href="#support">温柔工具</a>
          <a href="#about">关于小光</a>
        </nav>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow"><span /> 一盏不催你的小灯</p>
          <h1>今天不容易，<br /><em>也没关系。</em></h1>
          <p className="hero-text">
            不必马上振作，也不用把一切讲清楚。小光陪你停一停，看看此刻的心情，再走下一小步。
          </p>
          <div className="hero-actions">
            <button className="primary-button" onClick={scrollToCheckIn}>告诉小光此刻的心情</button>
            <a className="text-link" href="#support">先看看能做什么 <span aria-hidden="true">→</span></a>
          </div>
          <p className="privacy-note"><span aria-hidden="true">○</span> 本页体验无需登录，也不会上传你的选择</p>
        </div>
        <div className="light-scene" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="light-orb"><span>光</span></div>
          <p>我在，不急。</p>
        </div>
      </section>

      <section className="check-in section-shell" id="check-in">
        <div className="section-heading">
          <p className="eyebrow"><span /> 情绪温度计</p>
          <h2>现在的你，比较像哪一种天气？</h2>
          <p>没有正确答案。选一个最接近的就好。</p>
        </div>
        <div className="mood-panel">
          <div className="mood-options" role="radiogroup" aria-label="选择当前心情">
            {moods.map((mood, index) => (
              <button
                key={mood.label}
                className={selectedMood === index ? "mood-option selected" : "mood-option"}
                role="radio"
                aria-checked={selectedMood === index}
                onClick={() => setSelectedMood(index)}
              >
                <span className="mood-symbol" aria-hidden="true">{mood.symbol}</span>
                <span>{mood.label}</span>
              </button>
            ))}
          </div>
          <div className={selectedMood === null ? "mood-reply quiet" : "mood-reply"} aria-live="polite">
            <span className="reply-mark" aria-hidden="true">光</span>
            <p>{selectedMood === null ? "选好后，小光会在这里陪你说一句话。" : moods[selectedMood].reply}</p>
          </div>
        </div>
      </section>

      <section className="support section-shell" id="support">
        <div className="section-heading compact">
          <p className="eyebrow"><span /> 今天可以做的小事</p>
          <h2>不用解决全部，只照顾这一刻</h2>
        </div>
        <div className="support-grid">
          <article className="support-card featured">
            <span className="card-number">01</span>
            <h3>做三次慢呼吸</h3>
            <p>吸气时默数四拍，停一拍，再用六拍慢慢呼出。肩膀可以放松一点。</p>
            <span className="card-tag">约 1 分钟</span>
          </article>
          <article className="support-card">
            <span className="card-number">02</span>
            <h3>写下一句心情</h3>
            <p>“我现在感到____，可能因为____。” 不分析，也不评价，只是看见它。</p>
            <span className="card-tag">一行就够</span>
          </article>
          <article className="support-card">
            <span className="card-number">03</span>
            <h3>联系一个可信任的人</h3>
            <p>不知道怎么开口时，可以只发一句：“我今天有点难受，能陪我聊几分钟吗？”</p>
            <span className="card-tag">你不必独自撑着</span>
          </article>
        </div>
      </section>

      <section className="about section-shell" id="about">
        <div>
          <p className="eyebrow"><span /> 关于小光</p>
          <h2>陪伴，不是诊断</h2>
        </div>
        <div className="about-copy">
          <p>小光希望把心理照顾做得更轻、更日常：记录情绪、梳理感受、学习容易理解的心理知识。</p>
          <p>它不能替代医生或心理咨询师。如果你正处于危险中、有伤害自己的想法，请优先联系身边可信任的人，并拨打 <strong>120、110 或全国统一心理援助热线 12356</strong>。</p>
        </div>
      </section>

      <footer>
        <a className="brand footer-brand" href="#top"><span className="brand-mark" aria-hidden="true">光</span><span>小光心理助手</span></a>
        <p>愿你被允许，慢慢来。</p>
        <p className="footer-note">心理健康辅助体验 · 不提供医疗诊断</p>
      </footer>
    </main>
  );
}
