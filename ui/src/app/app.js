angular.module('app').component('app', {
    template: `
<h1>Hello World!</h1>
<span ng-if="bots">Found {{ bots.length }} bots.</span>
<ul>
    <li ng-repeat="bot in bots">{{ bot }}</li>
</ul>
`,
    controller: ($scope, $http) => {
        $http.get("http://localhost:8000/api/bots").then(resp => {
            $scope.bots = resp.data;
        });
    }
});
