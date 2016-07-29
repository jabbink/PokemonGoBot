angular
    .module('app', ['ui.router']);

// function routesConfig($stateProvider, $urlRouterProvider, $locationProvider) {
//     $locationProvider.html5Mode(true).hashPrefix('!');
//     $urlRouterProvider.otherwise('/');
//
//     $stateProvider
//         .state('app', {
//             url: '/',
//             template: '<app></app>'
//         });
// }

const myLayout = new GoldenLayout({
    settings: {
        hasHeaders: true
    },
    content: [{
        type: 'row',
        content: [{
            type: 'component',
            componentName: 'template',
            componentState: {templateId: 'test'}
        }]
    }]
});

myLayout.registerComponent('template', (container, state) => {
    const templateHtml = $(`#${state.templateId}`).html();
    container.getElement().html(templateHtml);
});

myLayout.on('initialised', () => {
    angular.bootstrap(document.body, ['app']);
});

myLayout.init();
