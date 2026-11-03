/* ═══════════════════════════════════════════════════════════════
   MathBuddy — app.js
   Voice AI Math Tutor + Child Profiles + Progress & Recommendations
   Style 5: Rainbow Classroom + Cute Owl Mascot
═══════════════════════════════════════════════════════════════ */

const API = '';

// ── Application State ────────────────────────────────────────────────────────
const state = {
  children:        [],
  activeChild:     null,      // { id, name, age, avatar, totalStars, ... }
  selectedTopic:   'addition',
  selectedDiff:    'easy',
  sessionId:       null,      // Current quiz session ID in MySQL
  question:        '',
  correctAnswer:   0,
  score:           0,
  questionsAttempted: 0,
  correctCount:    0,
  stars:           0,
  soundEnabled:    true,
  isListening:     false,
  voiceState:      'IDLE',    // IDLE | LISTENING | PROCESSING | SPEAKING | ERROR
  explanation:     null,      // Current MathExplanationDto for Owl's Math Adventure
  expCurrentStep:  0          // Current explanation step index
};

const AVATAR_MAP = {
  owl: '🦉', fox: '🦊', frog: '🐸',
  bunny: '🐰', lion: '🦁', bear: '🐻'
};

// ── Web Speech API: Speech Recognition (Voice In) ─────────────────────────────
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;

if (SpeechRecognition) {
  recognition = new SpeechRecognition();
  recognition.lang = 'en-US';
  recognition.interimResults = false;
  recognition.maxAlternatives = 1;

  recognition.onresult = async (event) => {
    const spokenText = event.results[0][0].transcript;
    console.log('🎤 Web Speech In:', spokenText);
    setVoiceState('PROCESSING', `Heard: "${spokenText}"`);
    updateRecognizedDisplay(spokenText);
    await handleVoiceInput(spokenText);
  };

  recognition.onerror = (event) => {
    console.warn('Speech recognition error:', event.error);
    setVoiceState('ERROR', 'Oops! Could not hear you. Tap mic to try again!');
  };

  recognition.onend = () => {
    if (state.voiceState === 'LISTENING') {
      setVoiceState('IDLE', 'Tap to talk!');
    }
  };
}

// ── Web Speech API: Speech Synthesis (Voice Out) ──────────────────────────────
function speak(text) {
  if (!state.soundEnabled || !window.speechSynthesis) return;

  window.speechSynthesis.cancel();
  // Strip emojis for clean vocal pronunciation
  const clean = text.replace(/[\u{1F600}-\u{1F64F}\u{1F300}-\u{1F5FF}\u{1F680}-\u{1F6FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/gu, '');
  const utterance = new SpeechSynthesisUtterance(clean);
  utterance.rate = 0.92;  // Gentle, friendly tempo for 4-6 year olds
  utterance.pitch = 1.15; // Cheerful, warm tutor voice
  utterance.volume = 1.0;
  utterance.lang = 'en-US';

  setVoiceState('SPEAKING', 'Barnaby is speaking... 🔊');
  utterance.onend = () => {
    setVoiceState('IDLE', 'Tap to talk!');
  };

  window.speechSynthesis.speak(utterance);
}

function toggleSound() {
  state.soundEnabled = !state.soundEnabled;
  const btn = document.getElementById('soundBtn');
  btn.textContent = state.soundEnabled ? '🔊 Voice ON' : '🔇 Voice OFF';
  if (!state.soundEnabled && window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }
}

// ── Voice UI State Controller ────────────────────────────────────────────────
function setVoiceState(vState, textMsg) {
  state.voiceState = vState;
  const btn = document.getElementById('quizMicBtn');
  const indicator = document.getElementById('voiceStatusIndicator');

  if (indicator) indicator.textContent = textMsg || 'Tap to talk!';

  if (btn) {
    if (vState === 'LISTENING') {
      btn.classList.add('listening');
      btn.textContent = '🎙️';
    } else {
      btn.classList.remove('listening');
      btn.textContent = '🎤';
    }
  }
}

function toggleVoice() {
  if (!recognition) {
    alert('Voice input is not supported in this browser. Please type or use quick chips!');
    return;
  }

  if (state.voiceState === 'LISTENING') {
    recognition.stop();
    setVoiceState('IDLE', 'Tap to talk!');
  } else {
    try {
      recognition.start();
      setVoiceState('LISTENING', "🔴 I'm listening! Speak now...");
    } catch (e) {
      console.error(e);
      setVoiceState('ERROR', 'Mic busy. Tap to try again!');
    }
  }
}

function updateRecognizedDisplay(text) {
  const el = document.getElementById('recognizedSpeechText');
  const input = document.getElementById('manualAnswerInput');
  if (el) el.textContent = `I heard: "${text}"`;
  if (input && !isNaN(Number(text.trim()))) {
    input.value = text.trim();
  }
}

function simulateVoice(phrase) {
  updateRecognizedDisplay(phrase);
  setVoiceState('PROCESSING', `Testing: "${phrase}"`);
  handleVoiceInput(phrase);
}

// ── Core LLM Intent Understanding Pipeline ────────────────────────────────────
async function handleVoiceInput(speechText) {
  if (!speechText) return;

  try {
    const payload = {
      childId:         state.activeChild ? state.activeChild.id : null,
      speechText:      speechText,
      sessionId:       state.sessionId,
      currentQuestion: state.question,
      currentAnswer:   state.correctAnswer,
      topic:           state.selectedTopic,
      difficulty:      state.selectedDiff
    };

    const res = await fetch(`${API}/api/voice/understand`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    console.log('🤖 Gemini Voice Intent Result:', data);

    // Show Gemini intent pill
    const pill = document.getElementById('quizIntentPill');
    const text = document.getElementById('quizIntentText');
    if (pill && text) {
      text.textContent = `${data.intent || 'DETECTED'} (${data.action || 'PROCESSED'})`;
      pill.classList.remove('hidden');
    }

    // Execute backend action result
    switch (data.action) {
      case 'QUIZ_STARTED':
        if (data.quizData) {
          applyQuizStarted(data.quizData);
        }
        break;

      case 'ANSWER_CHECKED':
        if (data.answerData) {
          applyAnswerResult(data.answerData);
        }
        break;

      case 'NEXT_QUESTION':
      case 'DIFFICULTY_CHANGED':
      case 'TOPIC_CHANGED':
        if (data.quizData) {
          applyNextQuestion(data.quizData);
        }
        break;

      case 'EXPLAINED':
        if (data.explainData) {
          if (data.explainData.details) {
            openExplanationPanel(data.explainData.details);
          } else {
            showExplanation(data.explainData.explanation);
          }
        }
        break;

      case 'REPEATED':
        // spokenResponse handles reading it
        break;
    }

    // Speak tutor response aloud
    if (data.spokenResponse) {
      speak(data.spokenResponse);
    }

  } catch (err) {
    console.error('Voice handling error:', err);
    setVoiceState('ERROR', "I had trouble understanding. Let's try again!");
  }
}

// ── Screen Navigation ────────────────────────────────────────────────────────
function showScreen(screenId) {
  const screens = ['screenProfiles', 'screenHome', 'screenQuiz', 'screenResults', 'screenProgress'];
  screens.forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      if (id === screenId) el.classList.remove('hidden');
      else el.classList.add('hidden');
    }
  });

  // Manage top nav button visibility
  const navChild = document.getElementById('activeChildPill');
  const navProg = document.getElementById('navProgressBtn');
  if (state.activeChild && screenId !== 'screenProfiles') {
    navChild.classList.remove('hidden');
    navProg.classList.remove('hidden');
    document.getElementById('navAvatar').textContent = AVATAR_MAP[state.activeChild.avatar] || '🦉';
    document.getElementById('navChildName').textContent = state.activeChild.name;
  } else {
    navChild.classList.add('hidden');
    navProg.classList.add('hidden');
  }

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goHome() {
  if (!state.activeChild) {
    showProfileSelect();
    return;
  }
  showScreen('screenHome');
  loadChildRecommendations();
}

function showProfileSelect() {
  showScreen('screenProfiles');
  fetchChildren();
}

function showProgressScreen() {
  if (!state.activeChild) return;
  showScreen('screenProgress');
  loadChildProgress(state.activeChild.id);
}

// ── Child Profiles Management ─────────────────────────────────────────────────
async function fetchChildren() {
  try {
    const res = await fetch(`${API}/api/children`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Could not fetch children`);
    state.children = await res.json();
    console.log('✅ Loaded child profiles:', state.children.length);
  } catch (e) {
    console.error('fetchChildren error:', e);
  } finally {
    renderChildrenGrid();
  }
}

function renderChildrenGrid() {
  const grid = document.getElementById('profilesGrid');
  if (!grid) {
    console.error('profilesGrid element not found!');
    return;
  }
  grid.innerHTML = '';

  if (Array.isArray(state.children)) {
    state.children.forEach(child => {
      const card = document.createElement('div');
      card.className = 'child-card';
      card.setAttribute('data-child-id', child.id);
      card.dataset.childId = child.id;
      card.onclick = () => selectChild(child.id);

      const avatarIcon = AVATAR_MAP[child.avatar] || '🦉';
      card.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
          <span style="font-size:0.75rem; font-weight:700; color:var(--mb-lav-dark); background:var(--mb-lav-light); padding:3px 10px; border-radius:12px;">ID #${child.id}</span>
          ${child.createdAt ? `<span style="font-size:0.7rem; color:var(--mb-text-muted);">${child.createdAt.split(',')[0]}</span>` : ''}
        </div>
        <div class="child-avatar-big">${avatarIcon}</div>
        <div class="child-name">${child.name}</div>
        <div class="child-age-badge">${child.age} years old</div>
        <div class="child-stats-row">
          <span>⭐ ${child.totalStars || 0}</span>
          <span>🎯 ${child.overallAccuracy || 0}%</span>
        </div>
      `;
      grid.appendChild(card);
    });
  }

  // "Add Learner" Card
  const addCard = document.createElement('div');
  addCard.className = 'child-card add-child-card';
  addCard.onclick = () => openNewProfileModal();
  addCard.innerHTML = `
    <div class="add-child-icon">+</div>
    <div class="child-name" style="font-size: 1.25rem;">New Learner</div>
    <div class="child-age-badge" style="background:var(--mb-sky);">Create Profile</div>
  `;
  grid.appendChild(addCard);
}

async function selectChild(childId) {
  const child = state.children.find(c => c.id === childId);
  if (!child) return;

  // 1. Completely clear previous child's profile-specific frontend state
  state.sessionId = null;
  state.question = '';
  state.correctAnswer = 0;
  state.score = 0;
  state.questionsAttempted = 0;
  state.correctCount = 0;
  state.stars = 0;

  // 2. Clear profile-specific UI elements to avoid stale data leakage
  const input = document.getElementById('manualAnswerInput');
  if (input) input.value = '';
  const speechText = document.getElementById('recognizedSpeechText');
  if (speechText) speechText.textContent = "I'm waiting to hear your voice! 🎙️";
  const feedbackBox = document.getElementById('quizFeedbackBox');
  if (feedbackBox) feedbackBox.className = 'feedback-box hidden';
  const recBanner = document.getElementById('homeRecBanner');
  if (recBanner) recBanner.classList.add('hidden');
  const recsContainer = document.getElementById('recsContainer');
  if (recsContainer) recsContainer.innerHTML = '';
  const masteryGrid = document.getElementById('masteryGrid');
  if (masteryGrid) masteryGrid.innerHTML = '';
  const sessionsBody = document.getElementById('parentSessionsBody');
  if (sessionsBody) sessionsBody.innerHTML = '';

  // 3. Set newly selected child identity
  state.activeChild = child;
  document.getElementById('homeWelcomeText').textContent =
    `Hi ${child.name}! What math game shall we play today? 🎈`;

  // 4. Update top nav with unique child identity
  document.getElementById('navAvatar').textContent = AVATAR_MAP[child.avatar] || '🦉';
  document.getElementById('navChildName').textContent = child.name;

  // 5. Load recommendations and view for this child only
  goHome();
  speak(`Welcome ${child.name}! Ready for some fun math games?`);
}

// ── Profile Modal (Create / Edit) ─────────────────────────────────────────────
function openNewProfileModal() {
  document.getElementById('childFormId').value = '';
  document.getElementById('modalTitle').textContent = 'New Learner Profile 🌟';
  document.getElementById('childNameInput').value = '';
  document.getElementById('childAgeInput').value = '5';
  selectAvatar('owl');
  document.getElementById('profileModal').classList.remove('hidden');
}

function openEditProfileModal() {
  if (!state.activeChild) return;
  document.getElementById('childFormId').value = state.activeChild.id;
  document.getElementById('modalTitle').textContent = `Edit ${state.activeChild.name}'s Profile ✏️`;
  document.getElementById('childNameInput').value = state.activeChild.name;
  document.getElementById('childAgeInput').value = state.activeChild.age;
  selectAvatar(state.activeChild.avatar || 'owl');
  document.getElementById('profileModal').classList.remove('hidden');
}

function closeProfileModal() {
  document.getElementById('profileModal').classList.add('hidden');
}

function selectAvatar(avatarName) {
  document.getElementById('selectedAvatar').value = avatarName;
  document.querySelectorAll('.avatar-choice').forEach(el => {
    if (el.getAttribute('data-avatar') === avatarName) el.classList.add('selected');
    else el.classList.remove('selected');
  });
}

async function saveChildProfile(event) {
  event.preventDefault();
  const submitBtn = event.target.querySelector('button[type="submit"]');
  if (submitBtn && submitBtn.disabled) return;
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Saving... ⏳';
  }

  const formId = document.getElementById('childFormId').value;
  const name = document.getElementById('childNameInput').value.trim();
  const age = parseInt(document.getElementById('childAgeInput').value, 10);
  const avatar = document.getElementById('selectedAvatar').value;

  if (!name) {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Save Profile ✨'; }
    return;
  }

  try {
    const isEdit = !!formId;
    const url = isEdit ? `${API}/api/children/${formId}` : `${API}/api/children`;
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, age, avatar })
    });

    if (!res.ok) throw new Error('Could not save profile');
    const saved = await res.json();
    closeProfileModal();

    await fetchChildren();
    selectChild(saved.id);

  } catch (err) {
    console.error(err);
    alert('Oops! Could not save the profile. Please try again!');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Save Profile ✨';
    }
  }
}

async function confirmDeleteChild() {
  if (!state.activeChild) return;
  const ok = confirm(`Are you sure you want to delete ${state.activeChild.name}'s profile? Their quiz history will be removed.`);
  if (!ok) return;

  try {
    const res = await fetch(`${API}/api/children/${state.activeChild.id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Could not delete profile');
    state.activeChild = null;
    showProfileSelect();
  } catch (e) {
    console.error(e);
    alert('Could not delete profile.');
  }
}

// ── Topic & Difficulty Selectors ──────────────────────────────────────────────
function selectTopic(topic) {
  state.selectedTopic = topic;
  const tiles = {
    addition: 'tileAddition',
    subtraction: 'tileSubtraction',
    multiplication: 'tileMultiplication',
    division: 'tileDivision'
  };
  Object.keys(tiles).forEach(t => {
    const el = document.getElementById(tiles[t]);
    if (el) {
      if (t === topic) el.classList.add('selected');
      else el.classList.remove('selected');
    }
  });
}

function selectDifficulty(diff) {
  state.selectedDiff = diff;
  ['diffEasy', 'diffMedium', 'diffHard'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.classList.remove('selected');
  });
  if (diff === 'easy') document.getElementById('diffEasy').classList.add('selected');
  if (diff === 'medium') document.getElementById('diffMedium').classList.add('selected');
  if (diff === 'hard') document.getElementById('diffHard').classList.add('selected');
}

// ── Quiz Lifecycle ────────────────────────────────────────────────────────────
async function startQuizManual() {
  if (!state.activeChild) {
    showProfileSelect();
    return;
  }

  showScreen('screenQuiz');
  setVoiceState('IDLE', 'Barnaby is getting your question...');

  try {
    const res = await fetch(`${API}/api/quiz/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        childId:    state.activeChild.id,
        topic:      state.selectedTopic,
        difficulty: state.selectedDiff
      })
    });

    if (!res.ok) throw new Error('Could not start quiz');
    const data = await res.json();
    applyQuizStarted(data);

    speak(`Here is your question: ${data.question}`);

  } catch (e) {
    console.error(e);
  }
}

function applyQuizStarted(data) {
  state.sessionId     = data.sessionId;
  state.question      = data.question;
  state.correctAnswer = data.answer;
  state.score         = 0;
  state.questionsAttempted = 0;
  state.correctCount  = 0;
  state.stars         = 0;

  showScreen('screenQuiz');
  renderQuestionDisplay(data.question, data.topic, data.difficulty);
}

function renderQuestionDisplay(question, topic, difficulty) {
  // Clear previous answer, feedback, and explanation panel
  document.getElementById('manualAnswerInput').value = '';
  document.getElementById('recognizedSpeechText').textContent = "I'm waiting to hear your voice! 🎙️";
  document.getElementById('quizFeedbackBox').className = 'feedback-box hidden';
  const panel = document.getElementById('mathExplanationPanel');
  if (panel) panel.classList.add('hidden');

  // Math text
  document.getElementById('quizMathQuestion').textContent = question;

  // Visual helper dots/fruits
  document.getElementById('quizVisualHelper').textContent = generateVisualHelper(question);

  // Badges
  const topicMap = {
    addition: '➕ Addition', subtraction: '➖ Subtraction',
    multiplication: '✖️ Multiplication', division: '➗ Division'
  };
  const diffMap = { easy: '🟢 Easy', medium: '🟡 Medium', hard: '🔴 Hard' };

  document.getElementById('quizNumberBadge').textContent = `Question ${state.questionsAttempted + 1}`;
  document.getElementById('quizTopicBadge').textContent = topicMap[topic] || topic;
  document.getElementById('quizDiffBadge').textContent = diffMap[difficulty] || difficulty;
  document.getElementById('quizScoreText').textContent = `Score: ${state.score} ⭐`;
  document.getElementById('owlQuizBubble').textContent = `What do you think is the answer to: ${question}?`;
}

function generateVisualHelper(question) {
  // Generates child-friendly visual items (apples, stars, balloons)
  try {
    const match = question.match(/(\d+)\s*([\+\-\×\÷\*\/])\s*(\d+)/);
    if (!match) return '🍎 🍎 🍎';
    const num1 = Math.min(parseInt(match[1]), 10);
    const op = match[2];
    const num2 = Math.min(parseInt(match[3]), 10);

    const icons = ['🍎', '⭐', '🎈', '🍪', '🌸'];
    const ic = icons[Math.floor(Math.random() * icons.length)];

    const part1 = ic.repeat(Math.max(num1, 1));
    const part2 = ic.repeat(Math.max(num2, 1));
    return `${part1}  ${op}  ${part2}`;
  } catch (e) {
    return '🍎🍎🍎';
  }
}

// ── Submit Answer ─────────────────────────────────────────────────────────────
async function submitManualAnswer() {
  const input = document.getElementById('manualAnswerInput');
  const ans = input.value.trim();
  if (!ans) return;

  try {
    const res = await fetch(`${API}/api/quiz/answer`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId:     state.sessionId,
        question:      state.question,
        userAnswer:    ans,
        correctAnswer: state.correctAnswer
      })
    });

    if (!res.ok) throw new Error('Answer validation error');
    const data = await res.json();
    applyAnswerResult(data);
  } catch (e) {
    console.error(e);
  }
}

function applyAnswerResult(data) {
  state.score = data.score;
  state.questionsAttempted = data.questionsAttempted;
  state.correctCount = data.correctAnswers;
  state.stars = data.starsEarned;

  document.getElementById('quizScoreText').textContent = `Score: ${data.score} ⭐`;

  const box = document.getElementById('quizFeedbackBox');
  const panel = document.getElementById('mathExplanationPanel');

  if (data.correct) {
    if (panel) panel.classList.add('hidden');
    box.className = 'feedback-box correct';
    box.classList.remove('hidden');
    document.getElementById('feedbackIcon').textContent = '🎉';
    document.getElementById('feedbackText').textContent = data.feedback;
    document.getElementById('feedbackCorrectAnswer').classList.add('hidden');
    document.getElementById('owlQuizBubble').textContent = "Hooray! That's correct! ⭐";
    speak(data.feedback);
  } else {
    // Incorrect answer: Launch "Owl's Math Adventure"
    box.classList.add('hidden');
    document.getElementById('owlQuizBubble').textContent = "Good try! Let's solve this together on Owl's Math Adventure! 🦉✨";

    if (data.explanation) {
      openExplanationPanel(data.explanation);
    } else {
      requestExplain();
    }
  }
}

// ── Next Question & Explain ───────────────────────────────────────────────────
async function nextQuestion() {
  if (!state.sessionId) return;

  try {
    const res = await fetch(`${API}/api/quiz/next`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: state.sessionId })
    });

    if (!res.ok) throw new Error('Could not get next question');
    const data = await res.json();
    applyNextQuestion(data);
    speak(`Next question: ${data.question}`);

  } catch (e) {
    console.error(e);
  }
}

function applyNextQuestion(data) {
  state.question = data.question;
  state.correctAnswer = data.answer;
  renderQuestionDisplay(data.question, data.topic, data.difficulty);
}

async function requestExplain() {
  if (!state.sessionId) return;

  try {
    const res = await fetch(`${API}/api/quiz/explain`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId:     state.sessionId,
        question:      state.question,
        topic:         state.selectedTopic,
        difficulty:    state.selectedDiff,
        userAnswer:    document.getElementById('manualAnswerInput').value || '?',
        correctAnswer: state.correctAnswer
      })
    });

    if (!res.ok) throw new Error('Could not get explanation');
    const data = await res.json();
    if (data.details) {
      openExplanationPanel(data.details);
    } else {
      showExplanation(data.explanation);
      speak(data.explanation);
    }

  } catch (e) {
    console.error(e);
  }
}

function showExplanation(explanationText) {
  const box = document.getElementById('quizFeedbackBox');
  box.className = 'feedback-box';
  box.style.background = 'var(--mb-lavender)';
  box.style.borderColor = 'var(--mb-lav-dark)';
  document.getElementById('feedbackIcon').textContent = '💡';
  document.getElementById('feedbackText').textContent = explanationText;
  document.getElementById('feedbackCorrectAnswer').classList.add('hidden');
  document.getElementById('owlQuizBubble').textContent = "Here is a hint to help you!";
}

// ── Owl's Math Adventure: Step-by-Step Explanation System ────────────────────

const VISUAL_ICONS = {
  apple: '🍎',
  cookie: '🍪',
  star: '⭐',
  balloon: '🎈',
  strawberry: '🍓',
  flower: '🌸'
};

function openExplanationPanel(explanationDto) {
  if (!explanationDto) return;
  state.explanation = explanationDto;
  state.expCurrentStep = 0;

  const panel = document.getElementById('mathExplanationPanel');
  if (!panel) return;

  // Set Title
  document.getElementById('expTitle').textContent = explanationDto.title || "Let's Solve This Together! ✨";

  // Build step dots
  const dotsContainer = document.getElementById('expDots');
  dotsContainer.innerHTML = '';
  const steps = explanationDto.steps || [];
  steps.forEach((_, idx) => {
    const dot = document.createElement('span');
    dot.className = `exp-dot ${idx === 0 ? 'active' : ''}`;
    dot.onclick = () => goToExplanationStep(idx);
    dotsContainer.appendChild(dot);
  });

  // Setup Follow-up Question
  const followUpBox = document.getElementById('expFollowUpBox');
  const followUpText = document.getElementById('expFollowUpQuestion');
  if (explanationDto.followUpQuestion && followUpBox && followUpText) {
    followUpText.textContent = explanationDto.followUpQuestion;
    followUpBox.classList.remove('hidden');
  } else if (followUpBox) {
    followUpBox.classList.add('hidden');
  }

  // Hide standard feedback box, show adventure panel
  document.getElementById('quizFeedbackBox').className = 'feedback-box hidden';
  panel.classList.remove('hidden');

  // Go to step 0
  goToExplanationStep(0);
}

function goToExplanationStep(stepIndex) {
  if (!state.explanation || !state.explanation.steps || state.explanation.steps.length === 0) return;
  const steps = state.explanation.steps;
  const total = steps.length;
  const idx = Math.max(0, Math.min(stepIndex, total - 1));
  state.expCurrentStep = idx;
  const step = steps[idx];

  // Update step badge
  document.getElementById('expStepBadge').textContent = `Step ${idx + 1} of ${total}`;

  // Update step narrative text
  document.getElementById('expStepText').textContent = step.text;

  // Update icon based on visual type
  const iconMap = {
    SHOW_GROUP: '👀',
    ADD_GROUP: '➕',
    REMOVE_GROUP: '✂️',
    COUNT_ITEMS: '🔢',
    COMPARE_GROUPS: '⚖️',
    NUMBER_PATH: '🐾',
    EQUATION: '🌟'
  };
  document.getElementById('expStepIcon').textContent = iconMap[step.visualType] || '💡';

  // Update dots
  const dots = document.querySelectorAll('.exp-dot');
  dots.forEach((dot, i) => {
    dot.className = 'exp-dot';
    if (i === idx) dot.classList.add('active');
    else if (i < idx) dot.classList.add('completed');
  });

  // Render Visual Math
  renderMathVisual(step, state.explanation.visualMeta);

  // Update navigation buttons
  const prevBtn = document.getElementById('expPrevBtn');
  const nextBtn = document.getElementById('expNextBtn');
  const nextQBtn = document.getElementById('expNextQuestionBtn');

  if (prevBtn) prevBtn.style.display = idx > 0 ? 'inline-block' : 'none';
  if (nextBtn) nextBtn.style.display = idx < total - 1 ? 'inline-block' : 'none';
  if (nextQBtn) {
    if (idx === total - 1) nextQBtn.classList.remove('hidden');
    else nextQBtn.classList.add('hidden');
  }

  // Speak step aloud
  speak(step.text);
}

function nextExplanationStep() {
  if (!state.explanation || !state.explanation.steps) return;
  if (state.expCurrentStep < state.explanation.steps.length - 1) {
    goToExplanationStep(state.expCurrentStep + 1);
  }
}

function prevExplanationStep() {
  if (state.expCurrentStep > 0) {
    goToExplanationStep(state.expCurrentStep - 1);
  }
}

function listenToCurrentStep() {
  if (!state.explanation || !state.explanation.steps) return;
  const step = state.explanation.steps[state.expCurrentStep];
  if (step) {
    speak(step.text);
  }
}

function replayFullExplanation() {
  if (!state.explanation || !state.explanation.steps) return;
  const fullText = (state.explanation.title || '') + '. ' +
    state.explanation.steps.map(s => s.text).join(' ') + ' ' +
    (state.explanation.finalExplanation || '');
  speak(fullText);
}

function tryQuestionAgain() {
  // Closes explanation panel and allows the child to retry
  const panel = document.getElementById('mathExplanationPanel');
  if (panel) panel.classList.add('hidden');

  const input = document.getElementById('manualAnswerInput');
  if (input) {
    input.value = '';
    input.focus();
  }
  const speechText = document.getElementById('recognizedSpeechText');
  if (speechText) speechText.textContent = "You've got this! Speak or type your answer! 🎙️";

  document.getElementById('owlQuizBubble').textContent = "Give it another try! I believe in you! 🌟";
  speak("Give it another try! I believe in you!");
}

function nextQuestionFromExplanation() {
  const panel = document.getElementById('mathExplanationPanel');
  if (panel) panel.classList.add('hidden');
  nextQuestion();
}

// ── MathVisual: Visual Math Representation Renderer ───────────────────────────
function renderMathVisual(step, meta) {
  const container = document.getElementById('mathVisualContainer');
  if (!container) return;
  container.innerHTML = '';

  const itemIcon = (meta && VISUAL_ICONS[meta.itemType]) ? VISUAL_ICONS[meta.itemType] : '🍎';
  const visualType = step.visualType || 'SHOW_GROUP';
  const pCount = Math.min(step.primaryCount || 0, 30);
  const sCount = Math.min(step.secondaryCount || 0, 30);
  const tCount = Math.min(step.totalCount || (pCount + sCount), 30);

  switch (visualType) {
    case 'SHOW_GROUP': {
      const grid = document.createElement('div');
      grid.className = 'visual-items-grid';
      for (let i = 0; i < pCount; i++) {
        const item = document.createElement('div');
        item.className = 'visual-item highlighted';
        item.textContent = itemIcon;
        grid.appendChild(item);
      }
      container.appendChild(grid);
      break;
    }

    case 'ADD_GROUP': {
      const row = document.createElement('div');
      row.className = 'visual-group-row';

      // Group 1
      const box1 = document.createElement('div');
      box1.className = 'visual-group-box';
      box1.innerHTML = `<div class="visual-group-title">Group 1 (${pCount})</div>`;
      const grid1 = document.createElement('div');
      grid1.className = 'visual-items-grid';
      for (let i = 0; i < pCount; i++) {
        const it = document.createElement('div');
        it.className = 'visual-item';
        it.textContent = itemIcon;
        grid1.appendChild(it);
      }
      box1.appendChild(grid1);
      row.appendChild(box1);

      // Plus symbol
      const plus = document.createElement('div');
      plus.className = 'visual-symbol';
      plus.textContent = '+';
      row.appendChild(plus);

      // Group 2 (Added / Highlighted)
      const box2 = document.createElement('div');
      box2.className = 'visual-group-box';
      box2.innerHTML = `<div class="visual-group-title">Added (${sCount})</div>`;
      const grid2 = document.createElement('div');
      grid2.className = 'visual-items-grid';
      for (let i = 0; i < sCount; i++) {
        const it = document.createElement('div');
        it.className = 'visual-item highlighted';
        it.textContent = itemIcon;
        grid2.appendChild(it);
      }
      box2.appendChild(grid2);
      row.appendChild(box2);

      container.appendChild(row);
      break;
    }

    case 'REMOVE_GROUP': {
      // Subtraction: Initial group where sCount items are marked removed
      const grid = document.createElement('div');
      grid.className = 'visual-items-grid';
      const keepCount = Math.max(0, pCount - sCount);

      for (let i = 0; i < pCount; i++) {
        const item = document.createElement('div');
        const isRemoved = i >= keepCount;
        item.className = `visual-item ${isRemoved ? 'removed' : 'highlighted'}`;
        item.textContent = itemIcon;
        grid.appendChild(item);
      }
      container.appendChild(grid);

      const note = document.createElement('div');
      note.style.marginTop = '10px';
      note.style.fontWeight = '800';
      note.style.color = 'var(--mb-lav-dark)';
      note.textContent = `${pCount} start - ${sCount} taken away = ${keepCount} left!`;
      container.appendChild(note);
      break;
    }

    case 'COUNT_ITEMS': {
      // Items with numbered tags: 1, 2, 3...
      const grid = document.createElement('div');
      grid.className = 'visual-items-grid';
      const count = tCount || pCount;
      for (let i = 1; i <= count; i++) {
        const item = document.createElement('div');
        item.className = 'visual-item highlighted';
        item.innerHTML = `${itemIcon}<span class="item-tag">${i}</span>`;
        grid.appendChild(item);
      }
      container.appendChild(grid);
      break;
    }

    case 'COMPARE_GROUPS': {
      const row = document.createElement('div');
      row.className = 'visual-group-row';

      const boxA = document.createElement('div');
      boxA.className = 'visual-group-box';
      boxA.innerHTML = `<div class="visual-group-title">Group A (${pCount})</div>`;
      const gridA = document.createElement('div');
      gridA.className = 'visual-items-grid';
      for (let i = 0; i < pCount; i++) {
        const it = document.createElement('div');
        it.className = 'visual-item';
        it.textContent = itemIcon;
        gridA.appendChild(it);
      }
      boxA.appendChild(gridA);
      row.appendChild(boxA);

      const vs = document.createElement('div');
      vs.className = 'visual-symbol';
      vs.textContent = 'vs';
      row.appendChild(vs);

      const boxB = document.createElement('div');
      boxB.className = 'visual-group-box';
      boxB.innerHTML = `<div class="visual-group-title">Group B (${sCount})</div>`;
      const gridB = document.createElement('div');
      gridB.className = 'visual-items-grid';
      for (let i = 0; i < sCount; i++) {
        const it = document.createElement('div');
        it.className = 'visual-item';
        it.textContent = itemIcon;
        gridB.appendChild(it);
      }
      boxB.appendChild(gridB);
      row.appendChild(boxB);

      container.appendChild(row);
      break;
    }

    case 'NUMBER_PATH': {
      const path = document.createElement('div');
      path.className = 'visual-number-path';
      const center = Math.max(1, pCount);
      for (let num = Math.max(1, center - 2); num <= center + 2; num++) {
        const stone = document.createElement('div');
        const isTarget = (num === center);
        stone.className = `path-stone ${isTarget ? 'missing' : ''}`;
        stone.textContent = isTarget ? `? (${num})` : num;
        path.appendChild(stone);
      }
      container.appendChild(path);
      break;
    }

    case 'EQUATION':
    default: {
      const banner = document.createElement('div');
      banner.className = 'visual-equation-banner';
      if (meta && meta.symbol) {
        banner.textContent = `${meta.num1} ${meta.symbol} ${meta.num2} = ${tCount}`;
      } else {
        banner.textContent = `${step.totalCount || tCount} ✨`;
      }
      container.appendChild(banner);
      break;
    }
  }
}

function repeatQuestion() {
  if (state.question) {
    speak(`The question is: ${state.question}`);
  }
}

async function finishQuizSession() {
  if (state.sessionId) {
    try {
      await fetch(`${API}/api/quiz/end`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId: state.sessionId })
      });
    } catch (e) {
      console.warn(e);
    }
  }

  showScreen('screenResults');
  document.getElementById('resScore').textContent = state.score;
  document.getElementById('resCorrect').textContent = `${state.correctCount} / ${state.questionsAttempted}`;

  const accuracy = state.questionsAttempted > 0
    ? Math.round((state.correctCount / state.questionsAttempted) * 100)
    : 100;
  document.getElementById('resAccuracy').textContent = `${accuracy}%`;

  const starCount = accuracy >= 90 ? 3 : (accuracy >= 60 ? 2 : 1);
  document.getElementById('resultsStars').textContent = '⭐'.repeat(starCount);

  speak(`Awesome quiz session! You earned ${starCount} stars and scored ${state.score} points!`);
}

// ── Child Progress & Personalized Recommendations ─────────────────────────────
async function loadChildRecommendations() {
  if (!state.activeChild) return;
  try {
    const res = await fetch(`${API}/api/children/${state.activeChild.id}/recommendations`);
    if (!res.ok) return;
    const recs = await res.json();

    const banner = document.getElementById('homeRecBanner');
    if (recs && recs.length > 0) {
      const topRec = recs[0];
      document.getElementById('homeRecIcon').textContent = topRec.icon || '🎯';
      document.getElementById('homeRecTitle').textContent = topRec.title;
      document.getElementById('homeRecMessage').textContent = topRec.message;
      banner.classList.remove('hidden');
      banner.setAttribute('data-topic', topRec.topic);
      banner.setAttribute('data-diff', topRec.difficulty);
    } else {
      banner.classList.add('hidden');
    }
  } catch (e) {
    console.error(e);
  }
}

function startRecommendedQuiz() {
  const banner = document.getElementById('homeRecBanner');
  const topic = banner.getAttribute('data-topic') || 'addition';
  const diff = banner.getAttribute('data-diff') || 'easy';
  selectTopic(topic);
  selectDifficulty(diff);
  startQuizManual();
}

async function loadChildProgress(childId) {
  try {
    const res = await fetch(`${API}/api/children/${childId}/progress`);
    if (!res.ok) throw new Error('Could not load child progress');
    const data = await res.json();

    document.getElementById('progressHeaderName').textContent = `${data.childName}'s Math Journey 🌟`;
    document.getElementById('progTotalStars').textContent = `${data.totalStars} ⭐`;
    document.getElementById('progAccuracy').textContent = `${data.overallAccuracy}%`;
    document.getElementById('progQuestions').textContent = data.totalQuestions;
    document.getElementById('progQuizzes').textContent = data.totalQuizzes;

    // Render Recommendations
    const recsBox = document.getElementById('recsContainer');
    recsBox.innerHTML = '';
    if (data.recommendations && data.recommendations.length > 0) {
      data.recommendations.forEach(r => {
        const div = document.createElement('div');
        div.className = 'rec-card';
        div.innerHTML = `
          <div class="rec-left">
            <div class="rec-icon">${r.icon || '🎯'}</div>
            <div>
              <div class="rec-title">${r.title}</div>
              <div class="rec-message">${r.message}</div>
            </div>
          </div>
          <button class="mb-btn mb-btn-small mb-btn-sky" onclick="practiceTopicFromRec('${r.topic}', '${r.difficulty}')">Play ✨</button>
        `;
        recsBox.appendChild(div);
      });
    }

    // Render Topic Mastery
    const masteryGrid = document.getElementById('masteryGrid');
    masteryGrid.innerHTML = '';
    if (data.topics) {
      data.topics.forEach(t => {
        const card = document.createElement('div');
        card.className = 'mastery-card';
        card.style.background = t.statusColor || 'var(--mb-cream)';
        card.innerHTML = `
          <div style="font-size:2.2rem;">${t.icon}</div>
          <div style="font-size:1.3rem; font-weight:900; margin:4px 0;">${capitalize(t.topic)}</div>
          <div style="font-size:0.9rem; font-weight:800; color:var(--mb-text);">${t.friendlyLabel}</div>
          <div class="mastery-progress-bar">
            <div class="mastery-progress-fill" style="width: ${t.accuracyPercentage}%"></div>
          </div>
          <div style="font-size:0.85rem; font-weight:700; color:var(--mb-text-muted);">
            ${t.correctAnswers} of ${t.totalAttempted} correct (${t.accuracyPercentage}%)
          </div>
        `;
        masteryGrid.appendChild(card);
      });
    }

    // Render Parent View Sessions Table
    const tbody = document.getElementById('parentSessionsBody');
    tbody.innerHTML = '';
    if (data.recentSessions && data.recentSessions.length > 0) {
      data.recentSessions.forEach(s => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${s.createdAt || 'Recent'}</td>
          <td>${capitalize(s.topic)}</td>
          <td>${capitalize(s.difficulty)}</td>
          <td>${s.questionsAttempted}</td>
          <td>${s.correctAnswers}</td>
          <td>${s.score}</td>
          <td>${'⭐'.repeat(s.starsEarned || 0) || '—'}</td>
        `;
        tbody.appendChild(tr);
      });
    } else {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:18px;">No sessions recorded yet!</td></tr>';
    }

  } catch (e) {
    console.error(e);
  }
}

function practiceTopicFromRec(topic, diff) {
  selectTopic(topic);
  selectDifficulty(diff);
  startQuizManual();
}

function toggleParentView() {
  const container = document.getElementById('parentHistoryContainer');
  const btn = document.getElementById('parentToggleBtn');
  if (container.classList.contains('hidden')) {
    container.classList.remove('hidden');
    btn.textContent = 'Parent View: Hide Table';
  } else {
    container.classList.add('hidden');
    btn.textContent = 'Parent View: Show Table';
  }
}

function capitalize(str) {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1);
}

function formatNumber(n) {
  if (n === null || n === undefined) return '0';
  return Number.isInteger(n) ? n : Number(n).toFixed(2);
}

// ── Startup Initialization ────────────────────────────────────────────────────
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    fetchChildren();
  });
} else {
  fetchChildren();
}
