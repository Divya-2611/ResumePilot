package com.resumepilot.util;

import java.util.List;
import java.util.Set;

/**
 * Curated dictionary of technology/skill phrases used to recognize skills and
 * tools inside job descriptions without an AI call.
 *
 * <p>Extend this list over time; it feeds the local extraction fallback
 * (the AI provider also extracts, and results are merged).</p>
 */
public final class SkillsDictionary {

    private SkillsDictionary() {
    }

    public static final List<String> SKILLS = List.of(
            "java", "javascript", "typescript", "python", "go", "golang", "rust", "c++",
            "c#", "csharp", "php", "ruby", "kotlin", "scala", "swift", "sql", "pl/sql",
            "bash", "shell scripting", "powershell", "html", "css", "scss", "sass",
            "react", "react.js", "angular", "vue", "vue.js", "svelte", "next.js", "nuxt",
            "node.js", "express", "spring", "spring boot", "hibernate", "jpa", "jdbc",
            "microservices", "rest", "rest api", "graphql", "grpc", "soap", "websocket",
            "docker", "kubernetes", "k8s", "helm", "terraform", "ansible", "jenkins",
            "ci/cd", "github actions", "gitlab ci", "maven", "gradle", "npm", "webpack",
            "vite", "aws", "azure", "gcp", "google cloud", "lambda", "ec2", "s3", "rds",
            "dynamodb", "cloudformation", "mysql", "postgresql", "postgres", "mongodb",
            "redis", "elasticsearch", "kafka", "rabbitmq", "cassandra", "oracle", "sqlite",
            "hive", "spark", "apache spark", "hadoop", "flink", "pandas", "numpy",
            "pytorch", "tensorflow", "keras", "scikit-learn", "machine learning", "ml",
            "deep learning", "nlp", "llm", "openai", "langchain", "data science",
            "data engineering", "etl", "data warehouse", "tableau", "power bi",
            "looker", "airflow", "databricks", "snowflake", "dbt", "agile", "scrum",
            "kanban", "jira", "confluence", "test driven development", "tdd",
            "junit", "mockito", "selenium", "cypress", "playwright", "pytest", "jest",
            "git", "svn", "linux", "unix", "windows", "macos", "azure devops",
            "oauth", "jwt", "saml", "ldap", "tls", "ssl", "owasp", "penetration testing",
            "tensorflow", "figma", "photoshop", "ui/ux", "product management",
            "leadership", "mentoring", "communication", "problem solving");

    public static final List<String> TOOLS = List.of(
            "intellij", "eclipse", "vs code", "visual studio", "postman", "swagger",
            "openapi", "gitlab", "github", "bitbucket", "slack", "teams", "zoom",
            "notion", "slack", "datagrip", "dbeaver", "chrome devtools", "sentry",
            "grafana", "prometheus", "kibana", "new relic", "datadog", "jfrog",
            "sonarqube", "nexus", "artifactory", "k6", "jmeter", "loadrunner",
            "vault", "keycloak", "auth0", "pagerduty", "opsgenie");

    public static final List<String> EDUCATION = List.of(
            "bachelor", "master", "ph.d", "phd", "mba", "bsc", "msc", "b.tech",
            "m.tech", "b.e", "m.e", "b.a", "m.a", "degree", "diploma", "certification",
            "computer science", "engineering", "information technology", "bca", "mca");

    public static final List<String> ACTION_VERBS = Set.of(
            "built", "designed", "developed", "implemented", "led", "managed", "created",
            "launched", "improved", "increased", "reduced", "optimized", "automated",
            "architected", "migrated", "delivered", "achieved", "spearheaded", "drove",
            "engineered", "deployed", "refactored", "integrated", "mentored", "collaborated",
            "coordinated", "established", "streamlined", "accelerated", "championed").stream().toList();
}
