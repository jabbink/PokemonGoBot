const gulp = require('gulp');
const filter = require('gulp-filter');
const useref = require('gulp-useref');
const rev = require('gulp-rev');
const revReplace = require('gulp-rev-replace');
const uglify = require('gulp-uglify');
const cssnano = require('gulp-cssnano');
const htmlmin = require('gulp-htmlmin');
const sourcemaps = require('gulp-sourcemaps');
const uglifySaveLicense = require('uglify-save-license');
const inject = require('gulp-inject');
const ngAnnotate = require('gulp-ng-annotate');

const conf = require('../conf/gulp.conf');

gulp.task('build', build);

function build() {
  const partialsInjectFile = gulp.src(conf.path.tmp('templateCacheHtml.js'), {read: false});
  const partialsInjectOptions = {
    starttag: '<!-- inject:partials -->',
    ignorePath: conf.paths.tmp,
    addRootSlash: false
  };

  const htmlFilter = filter(conf.path.tmp('*.html'), {restore: true});
  const jsFilter = filter(conf.path.tmp('**/*.js'), {restore: true});
  const cssFilter = filter(conf.path.tmp('**/*.css'), {restore: true});

  return gulp.src(conf.path.tmp('/index.html'))
    .pipe(inject(partialsInjectFile, partialsInjectOptions))
    .pipe(useref())
    .pipe(jsFilter)
    .pipe(sourcemaps.init())
    .pipe(ngAnnotate())
    .pipe(uglify({preserveComments: uglifySaveLicense})).on('error', conf.errorHandler('Uglify'))
    .pipe(rev())
    .pipe(sourcemaps.write('maps'))
    .pipe(jsFilter.restore)
    .pipe(cssFilter)
    .pipe(sourcemaps.init())
    .pipe(cssnano())
    .pipe(rev())
    .pipe(sourcemaps.write('maps'))
    .pipe(cssFilter.restore)
    .pipe(revReplace())
    .pipe(htmlFilter)
    .pipe(htmlmin())
    .pipe(htmlFilter.restore)
    .pipe(gulp.dest(conf.path.dist()));
}
