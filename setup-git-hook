#!/bin/sh

set_git_hooks()
{
    # assume that the current directory is the source tree
    if [ -d ".git" ] ; then
        for hook in $(ls -1 .git-hooks) ; do
            cd .git/hooks
            if [ ! -e "${hook?}" -o -L "${hook?}" ] ; then
                rm -f "${hook?}"
                ln -sf "../../.git-hooks/${hook?}" "${hook?}"
            fi
            cd - > /dev/null
        done
    fi
}

set_git_hooks
