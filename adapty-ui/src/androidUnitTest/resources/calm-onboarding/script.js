// Auto-generated — do not edit. Make changes in src/index.ts

var goToReviews = function () {
  SDK.openScreen({
    type: 'screen_reviews',
    instanceId: 'reviews',
    transitionId: 'forward',
  });
};
var goToGoals = function () {
  SDK.openScreen({
    type: 'screen_goals',
    instanceId: 'goals',
    transitionId: 'forward',
  });
};
var goToSurvey = function () {
  SDK.openScreen({
    type: 'screen_survey',
    instanceId: 'survey',
    transitionId: 'forward',
  });
};
var goToPaywall = function () {
  SDK.openScreen({
    type: 'screen_paywall',
    instanceId: 'paywall',
    transitionId: 'forward',
  });
};
var finishOnboarding = function () {
  SDK.closeAll();
};
var goal1Selected = false;
var goal2Selected = false;
var goal3Selected = false;
var goal4Selected = false;
var goal5Selected = false;
var goal6Selected = false;
var goal7Selected = false;
var toggleGoal1 = function () {
  goal1Selected = !goal1Selected;
};
var toggleGoal2 = function () {
  goal2Selected = !goal2Selected;
};
var toggleGoal3 = function () {
  goal3Selected = !goal3Selected;
};
var toggleGoal4 = function () {
  goal4Selected = !goal4Selected;
};
var toggleGoal5 = function () {
  goal5Selected = !goal5Selected;
};
var toggleGoal6 = function () {
  goal6Selected = !goal6Selected;
};
var toggleGoal7 = function () {
  goal7Selected = !goal7Selected;
};
var selectedSource = 'source_1';
var selectSource = function (params) {
  selectedSource = params.id;
};
var selectedTab = 'trial';
var tabIndex = 0;
function selectTrial() {
  selectedTab = 'trial';
  tabIndex = 0;
  SDK.moveScroll({
    instanceId: 'paywall',
    kind: 'content',
    value: 'end',
  });
}
function selectNoTrial() {
  selectedTab = 'no_trial';
  tabIndex = 1;
  SDK.moveScroll({
    instanceId: 'paywall',
    kind: 'content',
    value: 'end',
  });
}
var selectedProduct = 'individual';
function selectIndividual() {
  selectedProduct = 'individual';
}
function selectFamilyYearly() {
  selectedProduct = 'family_yearly';
}
function selectFamilyMonthly() {
  selectedProduct = 'family_monthly';
}
var reminderChecked = true;
function toggleReminder() {
  reminderChecked = !reminderChecked;
}
