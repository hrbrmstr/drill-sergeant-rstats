--- 
title: "Using Apache Drill with R"
author: "Bob Rudis"
date: "2018-03-29"
site: bookdown::bookdown_site
documentclass: book
bibliography: [book.bib]
biblio-style: apalike
link-citations: yes
github-repo: hrbrmstr/drill-sergeant-rstats
url: 'https\://rud.is/books/drill-sergeant-rstats/'
description: "A small guide to wrangling data to fuel analyses with Apache Drill and R"
---

# Preface {-}

[Apache Drill](https://drill.apache.org/) _"enables analysts, business users, data scientists and developers to explore and analyze this data without sacrificing the flexibility and agility offered by these datastores. Drill processes the data in-situ without requiring users to define schemas or transform data."_ ^[[Drill FAQ](https://drill.apache.org/faq/)]

This book is about using Apache Drill with R and the [`sergeant`](https://github.com/hrbrmstr/sergeant) package. Rather than produce an extensive tome, the chapters are organized into a more "cookbook"-style production with some (brief) introductory exposition.

Rather than write "I" everywhere, be it known up-front that any idioms or advocation contatained within are from the mind of someone who has stared into the heart of the TARDIS yet did not perish (i.e. I'm far askew of the average/normal individual and some things that work for me may seem strange, inefficient or even crazy to others). Emphatic/declarative statements should be interpreted as "I [think|believe]" vs accepted as fact without your own personal verification. For example, I'm about to say "Drill is&hellip;" in the next paragraph and it's all just my own delcarative take on this subject.

Drill is---in a word---_awesome_. On its own it can help you wrangle data out of (and into) parquet^[[Apache Parquet](https://parquet.apache.org/)], [CT]SV, JSON, MongoDB, MySQL, PostgreSQL (OK, anything that speaks JDBC), and more. You can scale it from a zero-configuration local filesystem install to a giant cluster with diverse data endpoints. You can speak SQL to it and have it translate into all the modern, heaven-forsaken NoSQL dialects for you (i.e. it can abstract away the query syntax of all the modern databases the crazy kids today use).

Drill lets you focus on a single way of thinking about data wrangling/processing so you can get to your analyses, visualizations, dasboards or reports more quickly. Drill also gives you an excuse to finally organize your data _without (necessarily) creating a massive single "data lake" in the process_. 

There are plenty of other reasons for "why Drill?"^[[Why Drill?](https://drill.apache.org/docs/why-drill/)], and you can jump into the active community^[[Drill Mailing Lists](https://drill.apache.org/mailinglists/)]  and even poke at the sources^[[Drill GitHub Source Mirror](https://github.com/apache/drill)] to gain a better understanding of the entirety of the mission/purpose of Drill.

The first few "recipes" will focus on Drill itself but the remainder of the book will be on solving specific problems or addressing specific situations with Drill & `sergeant`. If you need an area to be addressed that is not covered, please file an issue with as clear a question as possible and a new recipe will be added to help clarify each new use-case.

Also, the earlier drafts of this book will focus on Linux-ish systems (including macOS). I'll do my best to add Windows-specific context later on, but you'd be far better off installing Ubuntu in Windows 10 (since Microsoft supports that now).

## Preparing R for Drill

Using Drill with R relies on [`RJDBC`](https://www.rforge.net/RJDBC/) which means your system will need Java and [`rJava`](https://CRAN.R-project.org/package=rJava). You should spend some time on that now vs be frustrated later (if you do not currently have a working `rJava` environment). File an issue on this repo if having a recipe for getting Java working with R would be beneficial. (Kinder souls could also file a PR with said assistance.)

The `sergeant` package is available on CRAN but we may work with the GitHub version of it for this book in the event some enhancements are made to support recipes. If you prefer CRAN, then you can do:


```r
install.packages("sergeant")
```

otherwise, do:


```r
devtools::install_github("hrbrmstr/sergeant")
```

Every effort will be made to identify recipes that may require functionality not in the CRAN stable release.

This book also makes extensive use of the `tidyverse` meta-package, since one way to use `sergeant` is via the database back-end of `dplyr`. You will need to:


```r
install.packages("tidyverse")
```

if you have not used packages from it before (it may take a few minutes, especially on Linux systems).
