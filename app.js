const STORAGE_KEYS = {
  users: "ft_users",
  licenses: "ft_licenses",
  session: "ft_session",
};

const registerTab = document.getElementById("register-tab");
const loginTab = document.getElementById("login-tab");
const registerForm = document.getElementById("register-form");
const loginForm = document.getElementById("login-form");
const authMessage = document.getElementById("auth-message");
const licenseSection = document.getElementById("license-section");
const licenseForm = document.getElementById("license-form");
const licenseMessage = document.getElementById("license-message");
const welcomeMessage = document.getElementById("welcome-message");
const licenseList = document.getElementById("license-list");
const logoutButton = document.getElementById("logout-button");

let currentUser = null;

function readUsers() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.users)) || [];
  } catch (error) {
    console.warn("读取用户数据失败", error);
    return [];
  }
}

function saveUsers(users) {
  localStorage.setItem(STORAGE_KEYS.users, JSON.stringify(users));
}

function readLicenses() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.licenses)) || {};
  } catch (error) {
    console.warn("读取车牌数据失败", error);
    return {};
  }
}

function saveLicenses(licenses) {
  localStorage.setItem(STORAGE_KEYS.licenses, JSON.stringify(licenses));
}

function setSession(user) {
  if (user) {
    localStorage.setItem(STORAGE_KEYS.session, JSON.stringify({ username: user.username }));
  } else {
    localStorage.removeItem(STORAGE_KEYS.session);
  }
}

function getSession() {
  try {
    const data = JSON.parse(localStorage.getItem(STORAGE_KEYS.session));
    return data?.username || null;
  } catch (error) {
    return null;
  }
}

function showMessage(element, text, type = "") {
  element.textContent = text;
  element.classList.remove("success", "error");
  if (type) {
    element.classList.add(type);
  }
}

function switchTab(targetTab) {
  const isRegister = targetTab === "register";
  registerTab.classList.toggle("active", isRegister);
  loginTab.classList.toggle("active", !isRegister);
  registerForm.classList.toggle("active", isRegister);
  loginForm.classList.toggle("active", !isRegister);
  showMessage(authMessage, "");
}

function handleRegister(event) {
  event.preventDefault();
  const username = document.getElementById("register-username").value.trim();
  const password = document.getElementById("register-password").value.trim();

  if (username.length < 3) {
    showMessage(authMessage, "用户名至少需要 3 个字符", "error");
    return;
  }

  if (password.length < 6) {
    showMessage(authMessage, "密码至少需要 6 个字符", "error");
    return;
  }

  const users = readUsers();
  if (users.some((user) => user.username === username)) {
    showMessage(authMessage, "该用户名已被注册，请尝试其他名称。", "error");
    return;
  }

  const newUser = { username, password };
  users.push(newUser);
  saveUsers(users);
  showMessage(authMessage, "注册成功！请切换到登录标签。", "success");
  registerForm.reset();
}

function handleLogin(event) {
  event.preventDefault();
  const username = document.getElementById("login-username").value.trim();
  const password = document.getElementById("login-password").value.trim();
  const users = readUsers();
  const user = users.find((item) => item.username === username && item.password === password);

  if (!user) {
    showMessage(authMessage, "用户名或密码错误，请重试。", "error");
    return;
  }

  currentUser = user;
  setSession(user);
  loginForm.reset();
  enterLicenseMode();
}

function enterLicenseMode() {
  if (!currentUser) return;
  document.getElementById("auth-section").classList.add("hidden");
  licenseSection.classList.remove("hidden");
  welcomeMessage.textContent = `欢迎，${currentUser.username}！请登记车牌信息。`;
  showMessage(licenseMessage, "");
  refreshLicenseList();
}

function exitLicenseMode() {
  currentUser = null;
  setSession(null);
  document.getElementById("auth-section").classList.remove("hidden");
  licenseSection.classList.add("hidden");
  showMessage(authMessage, "您已退出登录。", "success");
}

function handleLicenseSubmit(event) {
  event.preventDefault();
  const licenseNumber = document.getElementById("license-number").value.trim().toUpperCase();
  const vehicleModel = document.getElementById("vehicle-model").value.trim();

  if (!currentUser) {
    showMessage(licenseMessage, "请先登录。", "error");
    return;
  }

  const licenses = readLicenses();
  const userLicenses = licenses[currentUser.username] || [];

  if (userLicenses.some((item) => item.licenseNumber === licenseNumber)) {
    showMessage(licenseMessage, "该车牌已登记，无需重复提交。", "error");
    return;
  }

  const entry = {
    licenseNumber,
    vehicleModel,
    createdAt: new Date().toISOString(),
  };

  userLicenses.push(entry);
  licenses[currentUser.username] = userLicenses;
  saveLicenses(licenses);
  showMessage(licenseMessage, "车牌登记成功！", "success");
  licenseForm.reset();
  refreshLicenseList();
}

function refreshLicenseList() {
  licenseList.innerHTML = "";
  const licenses = readLicenses();
  const userLicenses = licenses[currentUser.username] || [];

  if (userLicenses.length === 0) {
    licenseList.innerHTML = '<li>暂未登记任何车牌。</li>';
    return;
  }

  userLicenses
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .forEach((item) => {
      const listItem = document.createElement("li");
      const left = document.createElement("span");
      left.innerHTML = `<strong>${item.licenseNumber}</strong> - ${item.vehicleModel}`;

      const removeButton = document.createElement("button");
      removeButton.textContent = "删除";
      removeButton.classList.add("secondary");
      removeButton.addEventListener("click", () => removeLicense(item.licenseNumber));

      listItem.append(left, removeButton);
      licenseList.appendChild(listItem);
    });
}

function removeLicense(licenseNumber) {
  const licenses = readLicenses();
  const userLicenses = licenses[currentUser.username] || [];
  const updated = userLicenses.filter((item) => item.licenseNumber !== licenseNumber);
  licenses[currentUser.username] = updated;
  saveLicenses(licenses);
  refreshLicenseList();
  showMessage(licenseMessage, "车牌已删除。", "success");
}

registerTab.addEventListener("click", () => switchTab("register"));
loginTab.addEventListener("click", () => switchTab("login"));
registerForm.addEventListener("submit", handleRegister);
loginForm.addEventListener("submit", handleLogin);
licenseForm.addEventListener("submit", handleLicenseSubmit);
logoutButton.addEventListener("click", exitLicenseMode);

document.addEventListener("DOMContentLoaded", () => {
  const username = getSession();
  if (!username) {
    return;
  }

  const users = readUsers();
  const user = users.find((item) => item.username === username);
  if (user) {
    currentUser = user;
    enterLicenseMode();
  } else {
    setSession(null);
  }
});
