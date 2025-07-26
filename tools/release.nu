#!/usr/bin/env nu

const TYPES = ["major", "minor", "patch", "alpha", "beta", "rc", "release"]
const MAIN_TYPES = ["major", "minor", "patch"]

const TAG_PREFIX = "TCPD-"
const REMOTE = "tcpd"

def options [] {
    $TYPES
}

def mainOptions [] {
    $MAIN_TYPES
}


def bumpPatch [version] {
    $version | update patch (($version | get patch | into int) + 1 | into string)
}
def bumpMinor [version] {
    setMain $version patch "0" | update minor (($version | get minor | into int) + 1 | into string)
}
def bumpMajor [version] {
    setMain $version minor "0" | setMain $version patch "0" | update major (($version | get major | into int) + 1 | into string)
}

def setMain [version: record, kind: string, num: string] {
    $version | update $kind $num
}

def bumpMain [version, kind] {
    match $kind {
        "major" => (bumpMajor $version)
        "minor" => (bumpMinor $version)
        "patch" => (bumpPatch $version)
    }
}

def main [kind: string@options = "", secondaryKind?: string@mainOptions = "patch", --execute (-x)] {
    if $kind not-in $TYPES {
        error make -u {msg: $"Invalid kind `($kind)`. Must be one of: ($TYPES)"}
        return
    }
    if ($secondaryKind != null) and ($secondaryKind not-in $MAIN_TYPES) {
        error make -u {msg: $"Invalid secondary kind `($kind)`. Must be one of: ($TYPES)"}
        return
    }

    let buildGradleContent = open --raw ./build.gradle
    mut version = $buildGradleContent | lines | parse -r 'appVersionName = \u0027(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(?:(?:-rc.(?<rc>\d+))|(?:-alpha.(?<alpha>\d+))|(?:-beta.(?<beta>\d+)))?\u0027' | get 0
    mut versionCode = $buildGradleContent | lines | parse -r 'appVersionCode = (?<versionCode>\d+)' | get 0.versionCode | into int

    $versionCode = $versionCode + 1

    match $kind {
        "major" | "minor" | "patch" => ($version = bumpMain $version $kind),
        "alpha" | "beta" | "rc" => {
            let value = $version | get $kind
            if $value == "" {
                $version = bumpMain $version $secondaryKind
                $version = ($version | update $kind "1")
            } else {
                $version = ($version | update $kind ((($value | into int) + 1) | into string))
            }
            match $kind {
                "alpha" => {
                    if ($version.rc != "") or ($version.beta != "") {
                        error make -u {msg: "Cannot increment alpha version when rc or beta is set"}
                    }
                },
                "beta" => {
                    if ($version.rc != "") {
                        error make -u {msg: "Cannot increment beta version when rc is set"}
                    }
                    $version = ($version | update alpha "")
                },
                "rc" => {
                    $version = ($version | update alpha "")
                    $version = ($version | update beta "")
                }
            }
        }
        "release" => {
            if ($version.alpha == "") and ($version.beta == "") and ($version.rc == "") {
                error make -u {msg: "Kind `release` can only be used when alpha, beta or rc is currently set"}
            }
            $version = ($version | update alpha "")
            $version = ($version | update beta "")
            $version = ($version | update rc "")
        }
    }

    let versionString = $"($version.major).($version.minor).($version.patch)(
        if $version.alpha != "" {
            $"-alpha.($version.alpha)"
        } else if $version.beta != "" {
            $"-beta.($version.beta)"
        } else if $version.rc != "" {
            $"-rc.($version.rc)"
        }
    )"

    let tagName = $"($TAG_PREFIX)($versionString)"

    let changelogContent = open --raw ./CHANGELOG.md

    let changelogContent = $changelogContent | str replace -a Unreleased $versionString |
        str replace "...HEAD" $"...($tagName)" |
        str replace -a "ReleaseDate" (date now | format date "%Y-%m-%d") |
        str replace "<!-- next-header -->" "<!-- next-header -->\n\n## [Unreleased]\n\n> Released on ReleaseDate" |
        str replace "<!-- next-url -->" $"<!-- next-url -->\n[Unreleased]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/($tagName)...HEAD" |
        str replace -a " (by juh9870)" ""

    let buildGradleContent = $buildGradleContent | str replace -r "appVersionName = '[^']+'" $"appVersionName = '($versionString)'" |
        str replace -r "appVersionCode = \\d+" $"appVersionCode = ($versionCode)"

    if (which difft | is-empty) {
        $changelogContent | diff ./CHANGELOG.md -
        $buildGradleContent | diff ./build.gradle -
    } else {
        $changelogContent | difft ./CHANGELOG.md -
        $buildGradleContent | difft ./build.gradle -
    }

    let branch = git rev-parse --abbrev-ref HEAD
    let fullClarifiedName = $"(ansi attr_bold)(ansi yellow)($versionString)(ansi reset) with tag (ansi attr_bold)(ansi yellow)($tagName)(ansi reset) on branch (ansi attr_bold)($branch)(ansi reset)"

    print $"Releasing version: ($fullClarifiedName)\n"

    let dirty = ^git status --porcelain=1
    if $dirty != "" {
        print $"(ansi red)Working directory is dirty. Please commit or stash your changes(ansi reset)"
        print $dirty
        
        print ""
        print $"Aborting release ($versionString)"
        return
    }

    if not $execute {
        print "Dry run completed. Use -x to execute the changes."
        return
    }
    
    print $"Release version: ($fullClarifiedName)? \(y/n\)"
    loop {
        let key = (input listen --types [key])
        if ($key.code == 'y') and ($key.modifiers == []) {
            break
        } else if ($key.code == 'n') and ($key.modifiers == []) {
            print 'Cancelled'
            return
        } else if ($key.code == 'c') and ($key.modifiers == ['keymodifiers(control)']) {
            print 'Terminated with Ctrl-C'
            return
        } else {
            print "That key wasn't recognized."
            print 'Press (y) to release or (n) to Exit'
            continue
        }
    }

    $changelogContent | save -f ./CHANGELOG.md
    $buildGradleContent | save -f ./build.gradle

    print $"Committing changes"
    git add ./CHANGELOG.md ./build.gradle
    git commit -m $"Release: ($versionString)"
    git tag -a $tagName -m $"Release: ($versionString)"

    print $"Pushing changes"
    git push $REMOTE
    print $"Pushing tag"
    git push $REMOTE tag $tagName

    print $"Released version: ($fullClarifiedName)"
}