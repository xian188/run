/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.intellij.lang.annotations.Language
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.toml.isDependencyListHeader
import org.toml.lang.psi.*


class CargoTomlKeysCompletionProvider : CompletionProvider<CompletionParameters>() {
    private var cachedSchema: TomlSchema? = null

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val schema = cachedSchema
            ?: TomlSchema.parse(parameters.position.project, EXAMPLE_CARGO_TOML).also { cachedSchema = it }

        val key = parameters.position.parent as? TomlKeySegment ?: return
        val table = key.topLevelTable ?: return
        val variants = when (val parent = key.parent?.parent) {
            is TomlTableHeader -> {
                if (key != parent.key?.segments?.firstOrNull()) return
                val isArray = when (table) {
                    is TomlArrayTable -> true
                    is TomlTable -> false
                    else -> return
                }
                schema.topLevelKeys(isArray)
            }

            is TomlKeyValue -> {
                if (table !is TomlHeaderOwner) return
                if (table.header.isDependencyListHeader) return
                schema.keysForTable(table.name ?: return)
            }

            else -> return
        }

        result.addAllElements(variants.map {
            LookupElementBuilder.create(it)
        })
    }
}

private val TomlKeySegment.topLevelTable: TomlKeyValueOwner?
    get() {
        val table = ancestorStrict<TomlKeyValueOwner>() ?: return null
        if (table.parent !is TomlFile) return null
        return table
    }

private val TomlHeaderOwner.name: String?
    get() = header.key?.segments?.firstOrNull()?.name

// Example from https://doc.crates.io/manifest.html,
// basic completion is automatically generated from it.
@Language("TOML")
private val EXAMPLE_CARGO_TOML = """

[package]
name = "hello_world" # the name of the package
version = "0.1.0"    # the current version, obeying semver
authors = ["you@example.com"]
build = "build.rs"
documentation = "https://docs.rs/example"
exclude = ["build/**/*.o", "doc/**/*.html"]
include = ["src/**/*", "Cargo.toml"]
publish = false
workspace = "path/to/workspace/root"
edition = "2018"
rust-version = "1.56"  # supported in the coming 1.56 release

links = "..."
default-run = "..."
autobins = false
autoexamples = false
autotests = false
autobenches = false
resolver = "..."

description = "..."
homepage = "..."
repository = "..."
readme = "..."
keywords = ["...", "..."]
categories = ["...", "..."]
license = "..."
license-file = "..."

[badges]
appveyor = { repository = "...", branch = "master", service = "github" }
circle-ci = { repository = "...", branch = "master" }
gitlab = { repository = "...", branch = "master" }
travis-ci = { repository = "...", branch = "master" }
codecov = { repository = "...", branch = "master", service = "github" }
coveralls = { repository = "...", branch = "master", service = "github" }
is-it-maintained-issue-resolution = { repository = "..." }
is-it-maintained-open-issues = { repository = "..." }
maintenance = { status = "..." }

[profile.release]
opt-level = 3
debug = false
split-debuginfo = "..."
strip = "none"
rpath = false
lto = false
debug-assertions = false
codegen-units = 1
panic = 'unwind'
incremental = true
overflow-checks = true

[features]
default = ["jquery", "uglifier", "session"]

[workspace]
members = ["path/to/member1", "path/to/member2", "path/to/member3/*"]
exclude = ["path1", "path/to/dir2"]
default-members = ["path/to/member2", "path/to/member3/foo"]

[dependencies]
foo = { git = 'https://github.com/example/foo' }

[dev-dependencies]
tempdir = "0.3"

[build-dependencies]
gcc = "0.3"

[lib]
name = "foo"
path = "src/lib.rs"
crate-type = ["dylib", "staticlib", "cdylib", "rlib"]
test = true
doctest = true
bench = true
doc = true
plugin = false
proc-macro = false
harness = true
edition = "2018"

[[example]]
name = "foo"
path = "src/lib.rs"
test = true
doctest = true
bench = true
doc = true
plugin = false
harness = true
required-features = ["postgres", "tools"]
edition = "2018"

[[bin]]
name = "foo"
path = "src/lib.rs"
test = true
doctest = true
bench = true
doc = true
plugin = false
harness = true
required-features = ["postgres", "tools"]
edition = "2018"

[[test]]
name = "foo"
path = "src/lib.rs"
test = true
doctest = true
bench = true
doc = true
plugin = false
harness = true
required-features = ["postgres", "tools"]
edition = "2018"

[[bench]]
name = "foo"
path = "src/lib.rs"
test = true
doctest = true
bench = true
doc = true
plugin = false
harness = true
required-features = ["postgres", "tools"]
edition = "2018"

[patch.crates-io]
foo = { git = 'https://github.com/example/foo' }
"""
